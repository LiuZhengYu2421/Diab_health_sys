package com.imut.diab_health_sys01.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流器（内存实现，防止暴力破解 / 撞库）。
 * <p>
 * 规则：15 分钟计数窗口内，同一「IP + 用户名」连续失败 {@value #MAX_FAILS} 次，
 * 则锁定 {@link #LOCK_WINDOW_MS} 毫秒；期间登录直接拒绝。
 * <p>
 * 说明：内存实现适用于单机部署；若未来多实例部署，应替换为 Redis 计数（保留相同接口即可）。
 */
@Component
public class LoginRateLimiter {

    /** 计数窗口内允许的最大连续失败次数 */
    public static final int MAX_FAILS = 5;
    /** 锁定时长（毫秒）：15 分钟 */
    public static final long LOCK_WINDOW_MS = 15 * 60 * 1000L;
    /** 失败计数窗口（毫秒）：窗口内失败计入，超窗重置 */
    public static final long COUNT_WINDOW_MS = 15 * 60 * 1000L;

    /** key(ip|username) → long[]{ failCount, firstFailAt, lockUntil } */
    private final ConcurrentHashMap<String, long[]> store = new ConcurrentHashMap<>();

    /** 该 key 是否处于锁定状态 */
    public boolean isLocked(String key) {
        long[] v = store.get(key);
        if (v == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (v[2] > now) {
            return true;
        }
        // 锁定已过期，顺手清理
        if (v[2] != 0) {
            store.remove(key);
        }
        return false;
    }

    /** 剩余锁定毫秒数；未锁定返回 0 */
    public long remainingLockMs(String key) {
        long[] v = store.get(key);
        if (v == null) {
            return 0;
        }
        long remain = v[2] - System.currentTimeMillis();
        return remain > 0 ? remain : 0;
    }

    /** 当前失败次数（计数窗口内）；无记录返回 0 */
    public int getFailCount(String key) {
        long[] v = store.get(key);
        if (v == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        if (now - v[1] > COUNT_WINDOW_MS) {
            return 0;
        }
        return (int) v[0];
    }

    /**
     * 记录一次失败。
     *
     * @return 本次记录后是否已达到锁定阈值
     */
    public boolean recordFail(String key) {
        long now = System.currentTimeMillis();
        long[] v = store.compute(key, (k, old) -> {
            long[] cur = old;
            if (cur == null) {
                cur = new long[]{0, now, 0};
            } else if (now - cur[1] > COUNT_WINDOW_MS) {
                // 计数窗口已过，重新计数
                cur[0] = 0;
                cur[1] = now;
                cur[2] = 0;
            }
            cur[0]++;
            if (cur[0] >= MAX_FAILS) {
                cur[2] = now + LOCK_WINDOW_MS;
            }
            return cur;
        });
        return v[0] >= MAX_FAILS;
    }

    /** 登录成功后清除失败记录 */
    public void reset(String key) {
        store.remove(key);
    }
}
