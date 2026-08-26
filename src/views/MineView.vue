<template>
  <div class="content-page mine-page">
    <div class="panel-content">
      <!-- 个人信息 -->
      <section v-if="panel === 'profile'" class="panel-card">
        <div class="card-head">
          <div class="card-title"><i class="fa-solid fa-id-card"></i> 个人资料</div>
          <button class="ghost-btn" @click="editing = !editing">
            <i :class="editing ? 'fa-solid fa-xmark' : 'fa-solid fa-pen'"></i>{{ editing ? '取消' : '编辑' }}
          </button>
        </div>

        <div class="profile-grid">
          <div class="profile-item">
            <span class="label">头像</span>
            <div class="avatar-line">
              <img class="mini-avatar" :src="profileForm.avatar || userStore.avatar" alt="头像" @error="avatarError">
              <div v-if="editing" class="avatar-options">
                <img v-for="a in avatarOptions" :key="a" :src="a" :class="{ active: (profileForm.avatar || userStore.avatar) === a }" @click="profileForm.avatar = a" @error="avatarError">
              </div>
            </div>
          </div>
          <div class="profile-item">
            <span class="label">昵称</span>
            <input v-if="editing" v-model="profileForm.nickname" class="text-input" maxlength="20" placeholder="请输入昵称">
            <span v-else class="value">{{ userStore.displayName }}</span>
          </div>
          <div class="profile-item">
            <span class="label">用户名</span>
            <span class="value">{{ userStore.userInfo.username || userStore.userInfo.userName || '—' }}</span>
          </div>
          <div class="profile-item">
            <span class="label">角色</span>
            <span class="value">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</span>
          </div>
          <div class="profile-item">
            <span class="label">注册时间</span>
            <span class="value">{{ userStore.userInfo.createdAt || '—' }}</span>
          </div>
          <div class="profile-item">
            <span class="label">个性签名</span>
            <input v-if="editing" v-model="profileForm.desc" class="text-input" maxlength="30" placeholder="一句话介绍自己">
            <span v-else class="value">{{ userStore.displayDesc }}</span>
          </div>
        </div>

        <div v-if="editing" class="card-actions">
          <button class="primary-btn" @click="saveProfile"><i class="fa-solid fa-check"></i> 保存修改</button>
        </div>

        <!-- 糖尿病预测信息 -->
        <div class="card-head health-head">
          <div class="card-title"><i class="fa-solid fa-heart-pulse"></i> 健康档案</div>
          <button class="ghost-btn" @click="healthEditing = !healthEditing">
            <i :class="healthEditing ? 'fa-solid fa-xmark' : 'fa-solid fa-pen'"></i>{{ healthEditing ? '取消' : '编辑' }}
          </button>
        </div>

        <!-- 查看模式 -->
        <div v-if="!healthEditing" class="health-summary">
          <div class="health-grid">
            <div class="health-item">
              <span class="label">当前是否糖尿病</span>
              <span class="value">{{ healthForm.disease || '未填写' }}</span>
            </div>
            <div v-if="healthForm.disease === '是'" class="health-item">
              <span class="label">糖尿病类型</span>
              <span class="value">{{ healthForm.diabetesType || '—' }}</span>
            </div>
            <div class="health-item">
              <span class="label">性别</span>
              <span class="value">{{ healthForm.sex || '—' }}</span>
            </div>
            <div class="health-item">
              <span class="label">年龄</span>
              <span class="value">{{ healthForm.age ? healthForm.age + ' 岁' : '—' }}</span>
            </div>
            <div class="health-item">
              <span class="label">身高 / 体重</span>
              <span class="value">{{ healthForm.height ? healthForm.height + ' cm' : '—' }} / {{ healthForm.weight ? healthForm.weight + ' kg' : '—' }}</span>
            </div>
            <div class="health-item">
              <span class="label">家族病史</span>
              <span class="value">{{ healthForm.familyHistory || '未填写' }}</span>
            </div>
            <div class="health-item">
              <span class="label">腰围</span>
              <span class="value">
                {{ healthForm.waistline ? healthForm.waistline + ' cm' : (waistPredicted ? predictedWaist + ' cm' : '—') }}
                <em v-if="waistPredicted" class="predicted-tag">（预测）</em>
              </span>
            </div>
            <div class="health-item">
              <span class="label">收缩压</span>
              <span class="value">
                {{ healthForm.systolicPressure ? healthForm.systolicPressure + ' mmHg' : (bpPredicted ? predictedBp + ' mmHg' : '—') }}
                <em v-if="bpPredicted" class="predicted-tag">（预测）</em>
              </span>
            </div>
            <div class="health-item">
              <span class="label">是否处于妊娠期</span>
              <span class="value">{{ healthForm.isPregnancy || '—' }}</span>
            </div>
          </div>

          <!-- 风险评估建议 -->
          <div v-if="healthRisk.text" class="health-risk" :class="healthRisk.type">
            <div class="risk-head">
              <i :class="healthRisk.type === 'diag' ? 'fa-solid fa-stethoscope' : healthRisk.type === 'risk' ? 'fa-solid fa-gauge-high' : 'fa-solid fa-circle-info'"></i>
              <span v-if="healthRisk.title" class="risk-title">{{ healthRisk.title }}</span>
              <span v-if="healthRisk.score !== undefined" class="risk-score">风险评分 {{ healthRisk.score }}</span>
            </div>
            <p class="risk-text">{{ healthRisk.text }}</p>
          </div>

          <button class="primary-btn health-go-btn" @click="goRiskPredict"><i class="fa-solid fa-wand-magic-sparkles"></i> 去智能风险预测</button>
        </div>

        <!-- 编辑模式 -->
        <div v-else class="health-edit">
          <div class="health-edit-grid">
            <div class="health-field">
              <label>当前是否糖尿病</label>
              <div class="seg-group">
                <button class="seg" :class="{ on: healthForm.disease === '是' }" @click="healthForm.disease = '是'; healthForm.diabetesType = ''">是</button>
                <button class="seg" :class="{ on: healthForm.disease === '否' }" @click="healthForm.disease = '否'; healthForm.diabetesType = ''">否</button>
              </div>
            </div>
            <div v-if="healthForm.disease === '是'" class="health-field full">
              <label>糖尿病类型</label>
              <div class="seg-group">
                <button v-for="t in diabetesTypes" :key="t" class="seg" :class="{ on: healthForm.diabetesType === t }" @click="healthForm.diabetesType = t">{{ t }}</button>
              </div>
            </div>
            <div class="health-field">
              <label>性别</label>
              <div class="seg-group">
                <button class="seg" :class="{ on: healthForm.sex === '男' }" @click="healthForm.sex = '男'">男</button>
                <button class="seg" :class="{ on: healthForm.sex === '女' }" @click="healthForm.sex = '女'">女</button>
              </div>
            </div>
            <div class="health-field">
              <label>年龄</label>
              <input v-model.number="healthForm.age" type="number" class="text-input" min="1" max="120" placeholder="请输入年龄">
            </div>
            <div class="health-field">
              <label>身高 (cm)</label>
              <input v-model.number="healthForm.height" type="number" class="text-input" min="80" max="250" placeholder="e.g. 170">
            </div>
            <div class="health-field">
              <label>体重 (kg)</label>
              <input v-model.number="healthForm.weight" type="number" class="text-input" min="20" max="300" placeholder="e.g. 65">
            </div>
            <div class="health-field">
              <label>家族病史</label>
              <div class="seg-group">
                <button class="seg" :class="{ on: healthForm.familyHistory === '是' }" @click="healthForm.familyHistory = '是'">有</button>
                <button class="seg" :class="{ on: healthForm.familyHistory === '否' }" @click="healthForm.familyHistory = '否'">无</button>
              </div>
            </div>
            <div class="health-field">
              <label>腰围 (cm)</label>
              <div class="predict-input">
                <input v-model.number="healthForm.waistline" type="number" class="text-input" min="40" max="200" :placeholder="waistPredicted ? '预测约 ' + predictedWaist + ' cm' : '选填'">
                <span v-if="waistPredicted" class="predicted-tag">（预测）</span>
              </div>
            </div>
            <div class="health-field">
              <label>收缩压 (mmHg)</label>
              <div class="predict-input">
                <input v-model.number="healthForm.systolicPressure" type="number" class="text-input" min="60" max="250" :placeholder="bpPredicted ? '预测约 ' + predictedBp + ' mmHg' : '选填'">
                <span v-if="bpPredicted" class="predicted-tag">（预测）</span>
              </div>
            </div>
            <div class="health-field">
              <label>是否处于妊娠期</label>
              <div class="seg-group">
                <button class="seg" :class="{ on: healthForm.isPregnancy === '是' }" @click="healthForm.isPregnancy = '是'">是</button>
                <button class="seg" :class="{ on: healthForm.isPregnancy === '否' }" @click="healthForm.isPregnancy = '否'">否</button>
              </div>
            </div>
          </div>
          <div class="card-actions">
            <button class="primary-btn" @click="saveHealthInfo"><i class="fa-solid fa-check"></i> 保存预测信息</button>
          </div>
        </div>

        <!-- AI 服务配置（OpenAI）：一次填写，全站 AI 功能自动生效 -->
        <div class="card-head openai-head">
          <div class="card-title"><i class="fa-solid fa-robot"></i> AI 服务配置</div>
          <button v-if="openAiConfigured" class="ghost-btn" @click="clearOpenAiConfig">
            <i class="fa-solid fa-eraser"></i> 清除配置
          </button>
        </div>
        <div class="openai-form">
          <div class="openai-field">
            <label>OpenAI API Key <em>*</em></label>
            <div class="openai-key-line">
              <input v-model="openAiForm.apiKey" :type="showApiKey ? 'text' : 'password'" class="text-input" placeholder="sk-..." autocomplete="off" @input="openAiError = ''">
              <button class="ghost-btn eye-btn" type="button" @click="showApiKey = !showApiKey" :title="showApiKey ? '隐藏' : '显示'">
                <i :class="showApiKey ? 'fa-solid fa-eye-slash' : 'fa-solid fa-eye'"></i>
              </button>
            </div>
            <span class="openai-hint">必填，以 sk- 开头（如 sk-proj-xxxx…），用于调用 OpenAI 模型</span>
          </div>
          <div class="openai-field">
            <label>API Base URL</label>
            <input v-model="openAiForm.baseUrl" class="text-input" placeholder="请填写接口服务的 BaseURL 基础地址" autocomplete="off" @input="openAiError = ''">
            <span class="openai-hint">请填写接口服务的 BaseURL 基础地址。仅填写接口域名，请勿填写企业官网主页链接。</span>
          </div>
          <div class="openai-field">
            <label>模型名称</label>
            <input v-model="openAiForm.model" class="text-input" placeholder="选填，默认 gpt-4o-mini" autocomplete="off" @input="openAiError = ''">
            <span class="openai-hint">选填，默认 gpt-4o-mini</span>
          </div>
          <div class="openai-error" v-if="openAiError">{{ openAiError }}</div>
          <div class="openai-actions">
            <button class="primary-btn" @click="saveOpenAiConfig"><i class="fa-solid fa-floppy-disk"></i> 保存配置</button>
          </div>
          <div class="openai-tip">
            <i class="fa-solid fa-circle-info"></i>
            保存后，智能助手 / 医师咨询 / 风险预测 / 方案定制 / 健康建议等所有 AI 功能将自动使用您的 OpenAI 配置，无需逐个页面重复填写。
          </div>
        </div>

        <div class="card-head pwd-head">
          <div class="card-title"><i class="fa-solid fa-key"></i> 修改密码</div>
        </div>
        <div class="pwd-form">
          <input v-model="pwdForm.oldPassword" type="password" class="text-input" placeholder="原密码">
          <input v-model="pwdForm.newPassword" type="password" class="text-input" placeholder="新密码（6-32 位）">
          <input v-model="pwdForm.confirmPassword" type="password" class="text-input" placeholder="确认新密码">
          <button class="primary-btn" @click="savePassword"><i class="fa-solid fa-lock"></i> 确认修改</button>
        </div>
      </section>

      <!-- 我的方案 -->
      <section v-else-if="panel === 'plan'" class="panel-card">
        <div class="card-head">
          <div class="card-title"><i class="fa-solid fa-clipboard-list"></i> 我的健康方案</div>
          <button class="ghost-btn" @click="goLifeScheme"><i class="fa-solid fa-wand-magic-sparkles"></i> 去定制</button>
        </div>

        <div class="scheme-tabs">
          <button v-for="t in schemeTypes" :key="t" :class="{ active: schemeTab === t }" @click="switchSchemeTab(t)">{{ t }}方案</button>
        </div>

        <div v-if="schemeLoading" class="loading-view">
          <div class="loader"><span></span><span></span><span></span></div>
          <p>正在加载{{ schemeTab }}方案，请稍候…</p>
        </div>

        <div v-else-if="scheme" class="scheme-block">
          <div class="scheme-name">{{ scheme.name }}</div>
          <div class="scheme-desc">{{ scheme.desc }}</div>
          <div class="scheme-list">
            <div v-for="(item, i) in scheme.items" :key="i" class="scheme-item">
              <i :class="item.done ? 'fa-solid fa-circle-check done' : 'fa-regular fa-circle'"></i>
              <span class="scheme-time">{{ item.time }}</span>
              <span class="scheme-content">{{ item.content }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-view">
          <div class="empty-icon"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
          <p>暂无{{ schemeTab }}方案，前往「方案定制」让 AI 结合您的健康档案生成个性化{{ schemeTab }}方案</p>
          <button class="primary-btn" @click="goLifeScheme"><i class="fa-solid fa-play"></i> 去定制方案</button>
        </div>
      </section>

      <!-- 我的建议 -->
      <section v-else-if="panel === 'advice'" class="panel-card">
        <div class="card-head">
          <div class="card-title"><i class="fa-solid fa-heart-pulse"></i> 健康建议</div>
          <button class="ghost-btn" @click="generateAdvice" :disabled="adviceLoading">
            <i class="fa-solid fa-wand-magic-sparkles" :class="{ spinning: adviceLoading }"></i> 生成建议
          </button>
        </div>

        <div v-if="adviceLoading" class="loading-view">
          <div class="loader"><span></span><span></span><span></span></div>
          <p>正在结合您的健康档案生成建议，请稍候…</p>
        </div>

        <div v-else-if="hasAdvice" class="advice-groups">
          <div class="advice-group eat">
            <div class="group-title" @click="toggleAdviceGroup('eat')">
              <i class="fa-solid fa-bowl-food"></i> 饮食建议
              <i class="fa-solid fa-chevron-down group-arrow" :class="{ up: !adviceCollapsed.eat }"></i>
            </div>
            <div v-show="!adviceCollapsed.eat" class="advice-cards">
              <div v-for="(d, i) in adviceData.eat" :key="'eat' + i" class="advice-card eat">
                <div class="card-title-sm"><span class="advice-badge eat">{{ i + 1 }}</span>{{ d.title }}</div>
                <div class="card-text markdown-body" v-html="renderMarkdown(d.content)"></div>
              </div>
            </div>
          </div>
          <div class="advice-group sport">
            <div class="group-title" @click="toggleAdviceGroup('sport')">
              <i class="fa-solid fa-dumbbell"></i> 运动建议
              <i class="fa-solid fa-chevron-down group-arrow" :class="{ up: !adviceCollapsed.sport }"></i>
            </div>
            <div v-show="!adviceCollapsed.sport" class="advice-cards">
              <div v-for="(d, i) in adviceData.sport" :key="'sport' + i" class="advice-card sport">
                <div class="card-title-sm"><span class="advice-badge sport">{{ i + 1 }}</span>{{ d.title }}</div>
                <div class="card-text markdown-body" v-html="renderMarkdown(d.content)"></div>
              </div>
            </div>
          </div>
          <div class="advice-group daily">
            <div class="group-title" @click="toggleAdviceGroup('daily')">
              <i class="fa-solid fa-lightbulb"></i> 日常提醒
              <i class="fa-solid fa-chevron-down group-arrow" :class="{ up: !adviceCollapsed.daily }"></i>
            </div>
            <div v-show="!adviceCollapsed.daily" class="advice-cards">
              <div v-for="(d, i) in adviceData.daily" :key="'daily' + i" class="advice-card daily">
                <div class="card-title-sm"><span class="advice-badge daily">{{ i + 1 }}</span>{{ d.title }}</div>
                <div class="card-text markdown-body" v-html="renderMarkdown(d.content)"></div>
              </div>
            </div>
          </div>
          <div class="advice-group pop">
            <div class="group-title" @click="toggleAdviceGroup('pop')">
              <i class="fa-solid fa-book-open"></i> 控糖科普
              <i class="fa-solid fa-chevron-down group-arrow" :class="{ up: !adviceCollapsed.pop }"></i>
            </div>
            <div v-show="!adviceCollapsed.pop" class="advice-cards">
              <div v-for="(p, i) in adviceData.popularization" :key="'pop' + i" class="advice-card pop">
                <div class="card-title-sm"><span class="advice-badge pop">{{ i + 1 }}</span>{{ p.title }}</div>
                <div class="card-text markdown-body" v-html="renderMarkdown(p.content)"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空态：尚无建议 -->
        <div v-else class="empty-view">
          <div class="empty-icon"><i class="fa-solid fa-heart-pulse"></i></div>
          <p>还没有健康建议，点击「生成建议」让 AI 结合您的健康档案为您定制</p>
          <button class="primary-btn" @click="generateAdvice">
            <i class="fa-solid fa-wand-magic-sparkles"></i> 生成建议
          </button>
        </div>
      </section>

      <!-- 打卡记录 -->
      <section v-else-if="panel === 'check'" class="panel-card">
        <div class="card-head">
          <div class="card-title"><i class="fa-solid fa-calendar-check"></i> 打卡记录</div>
          <div class="head-actions">
            <button class="ghost-btn" @click="loadPunch"><i class="fa-solid fa-rotate" :class="{ spinning: punchLoading }"></i> 刷新</button>
            <button class="ghost-btn add-btn" @click="openPunchDialog"><i class="fa-solid fa-plus"></i> 新增打卡</button>
          </div>
        </div>

        <div v-if="punchLoading" class="loading-view">
          <div class="loader"><span></span><span></span><span></span></div>
          <p>正在加载打卡记录，请稍候…</p>
        </div>

        <template v-else>
          <div class="punch-stats">
            <div class="stat-box"><div class="stat-num warn">{{ punchStats.streak }}</div><div class="stat-label">连续天数</div></div>
            <div class="stat-box"><div class="stat-num">{{ punchStats.monthCount }}</div><div class="stat-label">本月打卡</div></div>
            <div class="stat-box"><div class="stat-num success">{{ punchStats.totalCount }}</div><div class="stat-label">累计打卡</div></div>
          </div>

          <div v-if="punchRecords.length" class="record-list">
            <div v-for="r in punchRecords" :key="r.id" class="record-item">
              <i :class="typeIcon(r.punchType)" class="record-icon" :style="{ color: typeColor(r.punchType) }"></i>
              <div class="record-info">
                <div class="record-type">
                  {{ r.punchType }}
                  <span v-if="r.message" class="record-message">{{ r.message }}</span>
                </div>
                <div class="record-date">{{ r.punchDate || r.createTime || r.createdAt || '' }}</div>
              </div>
              <span v-if="isDone(r.completionStatus)" class="status-badge ok">{{ formatStatus(r.completionStatus) }}</span>
              <button v-else class="complete-btn" :disabled="punchCompleting" @click="completePunch(r)">
                <i class="fa-solid fa-check"></i> 完成
              </button>
              <button class="record-del" :disabled="punchDeleting" @click="removePunch(r)">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </div>

          <div v-else class="empty-view">
            <div class="empty-icon"><i class="fa-solid fa-calendar-xmark"></i></div>
            <p>暂无打卡记录，快去完成今日打卡吧</p>
            <button class="primary-btn" @click="openPunchDialog"><i class="fa-solid fa-plus"></i> 新增打卡</button>
          </div>
        </template>
      </section>

      <!-- 新增打卡弹窗 -->
      <div v-if="punchDialog.show" class="punch-mask" @click.self="closePunchDialog">
        <div class="punch-dialog">
          <div class="punch-dialog-head">
            <h3><i class="fa-solid fa-calendar-plus"></i> 新增打卡</h3>
            <button class="close-btn" @click="closePunchDialog"><i class="fa-solid fa-xmark"></i></button>
          </div>
          <div class="punch-dialog-body">
            <div class="punch-form-item">
              <label>打卡类型</label>
              <div class="punch-type-group">
                <button
                  v-for="(m, key) in punchTypeMap"
                  :key="key"
                  class="punch-type-btn"
                  :class="{ on: punchForm.punchType === key }"
                  @click="punchForm.punchType = key"
                >
                  <i :class="m.icon" :style="{ color: m.color }"></i>
                  {{ key }}
                </button>
              </div>
            </div>
            <div class="punch-form-item">
              <label>备注</label>
              <input v-model.trim="punchForm.message" class="text-input" maxlength="50" placeholder="如：空腹血糖 5.6 mmol/L（选填）">
            </div>
            <div class="punch-tip">
              <i class="fa-solid fa-circle-info"></i> 新增打卡默认为「未完成」，打卡完成后可在列表中点击「完成」标记
            </div>
          </div>
          <div class="punch-dialog-foot">
            <button class="cancel-btn" @click="closePunchDialog">取消</button>
            <button class="confirm-btn" :disabled="punchSubmitting" @click="submitPunch">
              <i class="fa-solid fa-check"></i> {{ punchSubmitting ? '提交中…' : '确认打卡' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 我的资讯 -->
      <section v-else-if="panel === 'consult'" class="panel-card">
        <div class="card-head">
          <div class="card-title"><i class="fa-solid fa-bookmark"></i> 我的资讯</div>
          <button class="ghost-btn" @click="loadFavorites"><i class="fa-solid fa-rotate"></i> 刷新</button>
        </div>

        <div v-if="favorites.length" class="fav-list">
          <div v-for="(f, i) in favorites" :key="i" class="fav-item" @click="openFavorite(f)">
            <div class="fav-icon"><i class="fa-solid fa-file-lines"></i></div>
            <div class="fav-info">
              <div class="fav-title">{{ f.title }}</div>
              <div class="fav-meta">
                <span v-if="f.category" class="fav-cat">{{ f.category }}</span>
                <span class="fav-date"><i class="fa-solid fa-clock"></i> {{ f.savedAt || f.publishTime || '' }}</span>
              </div>
            </div>
            <button class="fav-remove" @click.stop="removeFavorite(f.title)">
              <i class="fa-solid fa-bookmark"></i> 已收藏
            </button>
          </div>
        </div>

        <div v-else class="empty-view">
          <div class="empty-icon"><i class="fa-solid fa-bookmark"></i></div>
          <p>暂无收藏的健康资讯，去「健康资讯」收藏感兴趣的内容吧</p>
          <button class="primary-btn" @click="goConsult"><i class="fa-solid fa-newspaper"></i> 去健康资讯</button>
        </div>

        <!-- 收藏详情弹窗 -->
        <div v-if="favDetail.show" class="fav-detail-mask" @click.self="closeFavoriteDetail">
          <div class="fav-detail-dialog">
            <div class="detail-head">
              <h3><i class="fa-solid fa-file-lines"></i> {{ favDetail.title }}</h3>
              <button class="close-btn" @click="closeFavoriteDetail"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div v-if="favDetail.tags.length" class="detail-tags">
              <span v-for="(t, i) in favDetail.tags" :key="i" class="tag"><i class="fa-solid fa-tag"></i>{{ t }}</span>
            </div>
            <div class="detail-content" v-html="favDetail.content"></div>
            <div class="detail-foot">
              <button class="unfav-btn" @click="unfavFromDetail">
                <i class="fa-solid fa-bookmark"></i> 取消收藏
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 帮助中心 -->
      <section v-else-if="panel === 'help'" class="panel-card">
        <div class="card-head">
          <div class="card-title"><i class="fa-solid fa-circle-question"></i> 帮助中心</div>
        </div>
        <div class="faq-card">
          <div v-for="(f, i) in faqList" :key="i" class="faq-item" :class="{ open: openFaq === i }" @click="openFaq = openFaq === i ? -1 : i">
            <div class="faq-question">
              <span><i class="fa-solid fa-circle-question"></i>{{ f.q }}</span>
              <i class="fa-solid fa-angle-down faq-arrow"></i>
            </div>
            <div class="faq-answer"><p>{{ f.a }}</p></div>
          </div>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { showFloatingAlert } from '@/utils/alert'
import { getLifePlans, getLifeAdvice, generateLifeAdvice, isMockMode } from '@/api/dify'
import { calcDiabetesRisk, predictWaist, predictBp, calcBmi } from '@/utils/diabetesRisk'
import { renderMarkdown } from '@/utils/markdown'
import { getAiErrorMessage } from '@/utils/aiError'
import { getPunchStats, getPunchRecords, createPunchRecord, deletePunchRecord, completePunchRecord } from '@/api/punchIn'
import { getMyFavorites, toggleFavoriteArticle } from '@/api/consult'
import { getConsultFavorites, removeConsultFavorite } from '@/utils/consultFavorites'
import { getToken } from '@/utils/storage'
import {
  refreshOpenAiConfig,
  saveOpenAiConfig as persistOpenAiConfig,
  clearOpenAiConfig as removePersistedOpenAiConfig,
  validateOpenAiConfig,
  OPENAI_DEFAULTS
} from '@/utils/openAiConfig'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 仅接受已知面板，未知值回退到个人信息，避免显示空白
const VALID_PANELS = ['profile', 'plan', 'advice', 'check', 'consult', 'help']
const panel = computed(() => {
  const p = route.query.panel
  return p && VALID_PANELS.includes(p) ? p : 'profile'
})

function avatarError(e) {
  e.target.src = '/img/user_icon.png'
}

/* ========== 个人信息 ========== */
const editing = ref(false)
const profileForm = ref({ nickname: '', avatar: '', desc: '' })
const avatarOptions = ['/img/user_icon.png', '/img/user1.png', '/img/user2.png', '/img/user.jpg', '/img/p3.png']

watch(
  () => userStore.userInfo,
  (info) => {
    profileForm.value = {
      nickname: info.nickname || userStore.displayName,
      avatar: info.avatar || info.avatarUrl || '',
      desc: info.desc || ''
    }
    editing.value = false
  },
  { immediate: true }
)

async function saveProfile() {
  const nickname = profileForm.value.nickname.trim()
  if (!nickname) {
    showFloatingAlert('昵称不能为空', 'warning')
    return
  }
  try {
    await userStore.updateProfile({
      nickname,
      avatar: profileForm.value.avatar || undefined,
      desc: profileForm.value.desc.trim() || undefined
    })
    showFloatingAlert('个人资料已更新', 'success')
    editing.value = false
  } catch (e) {
    showFloatingAlert(e.message || '保存失败，请稍后重试', 'error')
  }
}

/* ========== 糖尿病预测信息 ========== */
const diabetesTypes = ['1型糖尿病', '2型糖尿病', '妊娠糖尿病', '其他类型']
const healthEditing = ref(false)
const healthForm = ref({
  disease: '',
  diabetesType: '',
  sex: '',
  age: null,
  height: null,
  weight: null,
  familyHistory: '',
  waistline: null,
  systolicPressure: null,
  isPregnancy: ''
})

watch(
  () => userStore.userInfo,
  (info) => {
    const h = info.healthInfo || {}
    healthForm.value = {
      disease: h.disease || '',
      diabetesType: h.diabetesType || '',
      sex: h.sex || '',
      age: h.age ?? null,
      height: h.height ?? null,
      weight: h.weight ?? null,
      familyHistory: h.familyHistory || '',
      waistline: h.waistline ?? null,
      systolicPressure: h.systolicPressure ?? null,
      isPregnancy: h.isPregnancy || ''
    }
    healthEditing.value = false
  },
  { immediate: true }
)

async function saveHealthInfo() {
  const f = healthForm.value
  if (!f.disease) {
    showFloatingAlert('请选择当前是否为糖尿病', 'warning')
    return
  }
  if (f.disease === '是' && !f.diabetesType) {
    showFloatingAlert('请选择糖尿病类型', 'warning')
    return
  }
  if (!f.sex) {
    showFloatingAlert('请选择性别', 'warning')
    return
  }
  if (!f.age || f.age < 1) {
    showFloatingAlert('请填写正确的年龄', 'warning')
    return
  }
  if (!f.height || f.height < 80) {
    showFloatingAlert('请填写正确的身高', 'warning')
    return
  }
  if (!f.weight || f.weight < 20) {
    showFloatingAlert('请填写正确的体重', 'warning')
    return
  }
  if (f.waistline && (f.waistline < 40 || f.waistline > 200)) {
    showFloatingAlert('请填写正确的腰围', 'warning')
    return
  }
  if (f.systolicPressure && (f.systolicPressure < 60 || f.systolicPressure > 250)) {
    showFloatingAlert('请填写正确的收缩压', 'warning')
    return
  }
  try {
    await userStore.updateHealthInfo({ ...f })
    showFloatingAlert('糖尿病预测信息已保存', 'success')
    healthEditing.value = false
  } catch (e) {
    showFloatingAlert(e.message || '保存失败，请稍后重试', 'error')
  }
}

function goRiskPredict() {
  router.push('/risk-predict')
}

// 腰围/收缩压预测值（未填写时按身高体重性别推断，用于回显标注）
const predictedWaist = computed(() => {
  const f = healthForm.value
  if (!f.sex || !f.height) return null
  return predictWaist(f.sex, f.height, calcBmi(f.height, f.weight))
})
const predictedBp = computed(() => {
  const f = healthForm.value
  if (!f.sex || !f.height || !f.weight) return null
  return predictBp(f.sex, calcBmi(f.height, f.weight))
})
const waistPredicted = computed(() => !healthForm.value.waistline && predictedWaist.value !== null)
const bpPredicted = computed(() => !healthForm.value.systolicPressure && predictedBp.value !== null)

// 风险评估建议（根据已填写的预测信息实时计算，用于查看模式展示）
const healthRisk = computed(() => {
  const f = healthForm.value
  if (f.disease === '是') {
    return {
      type: 'diag',
      title: '已确诊',
      text: f.diabetesType ? `您已确诊为${f.diabetesType}，请遵医嘱规律治疗、保持健康生活方式，并定期复查随访。` : '您已填写确诊糖尿病，请选择糖尿病类型以获取更精准的建议。'
    }
  }
  if (f.disease === '否') {
    if (!f.sex || !f.age || !f.height || !f.weight) {
      return { type: 'empty', title: '', text: '请完善性别、年龄、身高、体重等信息后，系统将为您评估糖尿病风险。' }
    }
    const risk = calcDiabetesRisk(f)
    const levelText = risk.level === '高风险' ? '高风险' : risk.level === '中风险' ? '中风险' : '低风险'
    return { type: 'risk', level: risk.level, score: risk.total, title: `${levelText}风险`, text: risk.advice }
  }
  return { type: 'empty', title: '', text: '请先填写「当前是否糖尿病」，保存后将在此显示您的风险评估建议。' }
})

/* ========== AI 服务配置（OpenAI） ========== */
const openAiForm = ref({ apiKey: '', baseUrl: '', model: '' })
const showApiKey = ref(false)
const openAiConfigured = ref(false)
const openAiError = ref('')

async function initOpenAiForm() {
  // 数据库为权威存储：进入页面先从后端拉取配置到内存；后端不可用时回退本地非敏感元数据
  const cfg = await refreshOpenAiConfig()
  openAiConfigured.value = !!(cfg && (cfg.apiKey || cfg.hasApiKey))
  openAiForm.value = {
    // 后端可用时回显完整 Key（仅驻留内存/表单，不写 localStorage）；后端不可用时留空等待重新填写
    apiKey: cfg && cfg.apiKey ? cfg.apiKey : '',
    baseUrl: cfg ? cfg.baseUrl || '' : '',
    model: cfg ? cfg.model || '' : ''
  }
}
initOpenAiForm()

function saveOpenAiConfig() {
  const errors = validateOpenAiConfig(openAiForm.value)
  if (errors.apiKey) {
    openAiError.value = errors.apiKey
    showFloatingAlert(errors.apiKey, 'warning')
    return
  }
  if (errors.baseUrl) {
    openAiError.value = errors.baseUrl
    showFloatingAlert(errors.baseUrl, 'warning')
    return
  }
  if (errors.model) {
    openAiError.value = errors.model
    showFloatingAlert(errors.model, 'warning')
    return
  }
  const form = openAiForm.value
  persistOpenAiConfig({
    apiKey: form.apiKey.trim(),
    baseUrl: form.baseUrl.trim() || OPENAI_DEFAULTS.baseUrl,
    model: form.model.trim() || OPENAI_DEFAULTS.model
  })
  openAiConfigured.value = true
  openAiError.value = ''
  showFloatingAlert('AI 服务配置已保存，全站 AI 功能已生效', 'success')
}

function clearOpenAiConfig() {
  removePersistedOpenAiConfig()
  openAiConfigured.value = false
  openAiForm.value = { apiKey: '', baseUrl: '', model: '' }
  openAiError.value = ''
  showFloatingAlert('已清除 AI 服务配置，恢复默认 AI 服务', 'info')
}

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function savePassword() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword || !newPassword) {
    showFloatingAlert('请填写完整密码信息', 'warning')
    return
  }
  if (newPassword !== confirmPassword) {
    showFloatingAlert('两次输入的新密码不一致', 'warning')
    return
  }
  try {
    await userStore.changePassword({ oldPassword, newPassword })
    showFloatingAlert('密码修改成功', 'success')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    showFloatingAlert(e.message || '修改失败，请稍后重试', 'error')
  }
}

/* ========== 我的方案 ========== */
const schemeTypes = ['饮食', '运动']
const schemeTab = ref('饮食')
const scheme = ref(null)
const schemeLoading = ref(false)

function switchSchemeTab(t) {
  schemeTab.value = t
  scheme.value = null
  loadScheme()
}

async function loadScheme() {
  schemeLoading.value = true
  scheme.value = null
  try {
    // 个人中心展示"已生成"的方案（life_plans 表），不再重复调用 AI 生成
    const res = await getLifePlans(schemeTab.value)
    const data = isMockMode() ? res : (res.data || res)
    scheme.value = data.scheme || null
  } catch (e) {
    showFloatingAlert('方案加载失败，请稍后重试', 'error')
  } finally {
    schemeLoading.value = false
  }
}

/* ========== 我的建议（状态声明需在 watch(panel) immediate 之前初始化）========== */
const adviceData = ref({ eat: [], sport: [], daily: [], popularization: [] })
const adviceLoading = ref(false)
let adviceReqId = 0

/* ========== 打卡记录（状态声明需在 watch(panel) immediate 之前初始化）========== */
const punchStats = ref({ streak: 0, monthCount: 0, totalCount: 0 })
const punchRecords = ref([])
const punchLoading = ref(false)
const punchDialog = ref({ show: false })
const punchForm = ref({ punchType: '血糖监测', completionStatus: '未完成', message: '' })
const punchSubmitting = ref(false)
const punchDeleting = ref(false)
const punchCompleting = ref(false)

// 进入各面板时自动加载数据库数据：
// 首次进入（含 URL 直接带 ?panel=xxx）、从其他面板切换过来、从其他页面返回 均会触发加载
watch(panel, (p) => {
  if (p === 'plan') {
    loadScheme()
  }
  if (p === 'advice') {
    loadAdvice()
  }
  if (p === 'check') {
    loadPunch()
  }
}, { immediate: true })

/** 跳转方案定制页（在那里生成方案并写入 life_plans 表） */
function goLifeScheme() {
  router.push({ name: 'scheme' })
}

/* ========== 我的建议 ========== */
const hasAdvice = computed(() => {
  const g = adviceData.value
  return g.eat.length + g.sport.length + g.daily.length + g.popularization.length > 0
})

// 将 life_advice 条目（含 tags）按四类分组展示
function groupAdvice(list) {
  const groups = { eat: [], sport: [], daily: [], popularization: [] }
  ;(list || []).forEach((item) => {
    const tags = item.tags || ''
    if (tags.includes('饮食')) groups.eat.push(item)
    else if (tags.includes('运动')) groups.sport.push(item)
    else if (tags.includes('日常')) groups.daily.push(item)
    else if (tags.includes('科普')) groups.popularization.push(item)
    else groups.daily.push(item)
  })
  return groups
}

// 建议分组折叠状态（false = 展开，true = 收起）
const adviceCollapsed = reactive({ eat: false, sport: false, daily: false, popularization: false })

// 点击分组标题切换展开/收起
function toggleAdviceGroup(group) {
  adviceCollapsed[group] = !adviceCollapsed[group]
}

// 进入「我的建议」时查询已生成建议；没有则自动生成
async function loadAdvice() {
  const reqId = ++adviceReqId
  adviceLoading.value = true
  try {
    const res = await getLifeAdvice()
    if (reqId !== adviceReqId) return // 已过期，丢弃
    const data = isMockMode() ? res : (res.data || res)
    adviceData.value = groupAdvice(data.advice || [])
    if (!hasAdvice.value) {
      await generateAdvice(true, reqId)
    }
  } catch (e) {
    if (reqId !== adviceReqId) return
    showFloatingAlert('建议加载失败，请稍后重试', 'error')
  } finally {
    if (reqId === adviceReqId) adviceLoading.value = false
  }
}

// 生成建议（「生成建议」按钮 / 进入面板自动）：后端按健康档案调用工作流并落库 life_advice
async function generateAdvice(force = false, reqId = null) {
  if (!force && adviceLoading.value) return
  const id = reqId || ++adviceReqId
  adviceLoading.value = true
  try {
    await generateLifeAdvice()
    if (id !== adviceReqId) return // 已过期，丢弃
    // 生成成功后重新从数据库查询，确保展示最新落库数据（避免并发时响应被丢弃导致页面空态）
    const listRes = await getLifeAdvice()
    if (id !== adviceReqId) return
    const listData = isMockMode() ? listRes : (listRes.data || listRes)
    adviceData.value = groupAdvice(listData.advice || [])
    showFloatingAlert('健康建议已生成', 'success')
  } catch (e) {
    if (id !== adviceReqId) return
    showFloatingAlert(getAiErrorMessage(e, e.message || '建议生成失败，请稍后重试'), 'error')
  } finally {
    if (id === adviceReqId) adviceLoading.value = false
  }
}

/* ========== 打卡记录 ========== */
const punchTypeMap = {
  '血糖监测': { icon: 'fa-solid fa-droplet', color: '#3b82f6' },
  '饮食': { icon: 'fa-solid fa-bowl-food', color: '#16a34a' },
  '运动': { icon: 'fa-solid fa-dumbbell', color: '#f59e0b' },
  '作息': { icon: 'fa-solid fa-moon', color: '#8b5cf6' }
}

function openPunchDialog() {
  punchForm.value = { punchType: '血糖监测', completionStatus: '未完成', message: '' }
  punchDialog.value.show = true
}

function closePunchDialog() {
  if (punchSubmitting.value) return
  punchDialog.value.show = false
}

async function submitPunch() {
  if (!punchForm.value.punchType) {
    showFloatingAlert('请选择打卡类型', 'warning')
    return
  }
  punchSubmitting.value = true
  try {
    // 新增打卡一律为「未完成」，完成后在列表中点击「完成」标记
    await createPunchRecord({
      punchType: punchForm.value.punchType,
      completionStatus: '未完成',
      message: punchForm.value.message || undefined
    })
    showFloatingAlert('打卡成功', 'success')
    await loadPunch()
    punchDialog.value.show = false
  } catch (e) {
    showFloatingAlert(e.message || '打卡失败，请稍后重试', 'error')
  } finally {
    punchSubmitting.value = false
  }
}

async function removePunch(r) {
  punchDeleting.value = true
  try {
    await deletePunchRecord(r.id)
    const idx = punchRecords.value.findIndex((x) => x.id === r.id)
    if (idx > -1) punchRecords.value.splice(idx, 1)
    punchStats.value.totalCount = Math.max(0, punchStats.value.totalCount - 1)
    showFloatingAlert('打卡记录已删除', 'success')
  } catch (e) {
    showFloatingAlert(e.message || '删除失败，请稍后重试', 'error')
  } finally {
    punchDeleting.value = false
  }
}

// 标记打卡为已完成
async function completePunch(r) {
  punchCompleting.value = true
  try {
    await completePunchRecord(r.id)
    const target = punchRecords.value.find((x) => x.id === r.id)
    if (target) target.completionStatus = '已完成'
    showFloatingAlert('已标记为完成', 'success')
  } catch (e) {
    showFloatingAlert(e.message || '操作失败，请稍后重试', 'error')
  } finally {
    punchCompleting.value = false
  }
}

function typeIcon(type) {
  return (punchTypeMap[type] || { icon: 'fa-solid fa-check' }).icon
}
function typeColor(type) {
  return (punchTypeMap[type] || { color: '#64748b' }).color
}
function isDone(status) {
  return String(status) === '已完成' || Number(status) === 1 || status === true
}
function formatStatus(status) {
  return isDone(status) ? '已完成' : '未完成'
}

async function loadPunch() {
  punchLoading.value = true
  try {
    const [statsRes, recordsRes] = await Promise.all([getPunchStats(), getPunchRecords({ page: 1, pageSize: 50 })])
    const s = statsRes.data || statsRes || {}
    punchStats.value = { streak: s.streak || 0, monthCount: s.monthCount || 0, totalCount: s.totalCount || 0 }
    const r = recordsRes.data || recordsRes || {}
    punchRecords.value = r.list || r.records || r.rows || (Array.isArray(r) ? r : [])
  } catch (e) {
    showFloatingAlert('打卡记录加载失败，请稍后重试', 'error')
  } finally {
    punchLoading.value = false
  }
}

/* ========== 我的资讯（收藏） ========== */
const favorites = ref([])
const favDetail = reactive({ show: false, title: '', content: '', tags: [] })

async function loadFavorites() {
  // 未登录时回退到本地收藏
  if (!getToken()) {
    favorites.value = getConsultFavorites()
    return
  }
  try {
    const res = await getMyFavorites()
    favorites.value = Array.isArray(res) ? res : (res && res.list) || []
  } catch (e) {
    favorites.value = []
  }
}

function openFavorite(f) {
  favDetail.show = true
  favDetail.title = f.title
  favDetail.content = f.content || '<p>该收藏内容为空。</p>'
  favDetail.tags = f.tags || []
}

function closeFavoriteDetail() {
  favDetail.show = false
}

async function removeFavorite(title) {
  const item = favorites.value.find((f) => f.title === title)
  try {
    if (getToken() && item && item.articleId) {
      await toggleFavoriteArticle({ articleId: item.articleId, title: item.title })
    } else {
      removeConsultFavorite(title)
    }
  } catch (e) {
    // 后端失败时同步移除本地兜底收藏
    removeConsultFavorite(title)
  }
  await loadFavorites()
  if (favDetail.show && favDetail.title === title) favDetail.show = false
  showFloatingAlert('已取消收藏', 'info')
}

async function unfavFromDetail() {
  await removeFavorite(favDetail.title)
  favDetail.show = false
}

function goConsult() {
  router.push('/lifeadvice')
}

// 进入「我的资讯」面板时刷新收藏列表
watch(
  () => route.query.panel,
  (p) => {
    if (p === 'consult') loadFavorites()
  },
  { immediate: true }
)

// ========== 帮助中心 ==========
const faqList = [
  { q: '如何修改个人资料？', a: '进入「个人中心 → 个人信息」，点击「编辑」即可修改昵称、头像与个性签名，保存后立即生效。' },
  { q: '如何定制健康方案？', a: '在「个人中心 → 我的方案」或「方案定制」中填写个人信息与生活习惯，点击「生成方案」，AI 将结合您的信息生成个性化方案。' },
  { q: '如何查看打卡记录？', a: '在「个人中心 → 打卡记录」中可查看连续天数、本月打卡与累计打卡统计，以及全部打卡明细。' },
  { q: '如何修改登录密码？', a: '在「个人中心 → 个人信息」底部填写原密码与新密码，确认后即可完成修改。' },
  { q: '健康建议多久更新？', a: '健康建议由 AI 根据您的健康档案实时生成，可随时点击「刷新建议」获取最新内容。' },
  { q: '数据安全如何保障？', a: '您的健康数据仅用于个性化健康服务，平台采用登录认证机制保护账户信息安全。' }
]
const openFaq = ref(0)

</script>

<style scoped>
.mine-page {
  display: flex;
  flex-direction: column;
  padding: 24px 26px;
  background: #eef3fa;
  border-radius: 18px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
/* 面板内容 */
.panel-content { flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; }
.panel-card {
  background: #fff; border-radius: 14px; padding: 20px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
}
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e3a5f; }
.card-title i { color: #2563eb; }
.card-actions { margin-top: 16px; }
.ghost-btn {
  padding: 7px 14px; border: 1px solid #dbeafe; border-radius: 8px;
  background: #eff6ff; color: #2563eb; font-size: 12.5px; cursor: pointer;
  transition: all 0.2s;
}
.ghost-btn:hover { background: #dbeafe; }
.primary-btn {
  padding: 9px 22px; border: none; border-radius: 20px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff; font-size: 13px; cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transition: transform 0.2s;
}
.primary-btn:hover { transform: translateY(-1px); }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 个人信息：自适应列数（容器 880px 下约 4 列，窄屏自动减少） */
.profile-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 14px 24px; }
.profile-item { display: flex; flex-direction: column; gap: 6px; }
.profile-item .label { font-size: 12px; color: #94a3b8; }
.profile-item .value { font-size: 14px; color: #1e293b; }
.text-input {
  padding: 9px 12px; border: 1px solid #e2e8f0; border-radius: 8px;
  font-size: 13.5px; color: #1e293b; outline: none; background: #f8fafc;
  transition: all 0.2s; width: 100%; box-sizing: border-box;
}
.text-input:focus { border-color: #2563eb; background: #fff; }
.avatar-line { display: flex; align-items: center; gap: 12px; }
.mini-avatar { width: 56px; height: 56px; border-radius: 12px; object-fit: cover; border: 1px solid #e2e8f0; }
.avatar-options { display: flex; gap: 8px; flex-wrap: wrap; }
.avatar-options img {
  width: 44px; height: 44px; border-radius: 10px; object-fit: cover;
  border: 2px solid transparent; cursor: pointer; transition: all 0.2s;
}
.avatar-options img:hover { border-color: #93c5fd; }
.avatar-options img.active { border-color: #2563eb; box-shadow: 0 0 0 2px #dbeafe; }

.pwd-head { margin-top: 24px; }
.pwd-form { display: flex; gap: 10px; flex-wrap: wrap; }
.pwd-form .text-input { flex: 1; min-width: 160px; }

/* AI 服务配置 */
.openai-head { margin-top: 24px; }
.openai-head .card-title i { color: #7c3aed; }
.openai-form { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px 20px; }
.openai-field { display: flex; flex-direction: column; gap: 6px; }
.openai-field label { font-size: 12.5px; color: #475569; }
.openai-field label em { color: #ef4444; font-style: normal; }
.openai-key-line { display: flex; gap: 8px; }
.openai-key-line .text-input { flex: 1; min-width: 0; }
.eye-btn { flex-shrink: 0; padding: 8px 12px; display: flex; align-items: center; justify-content: center; }
.openai-hint { font-size: 11.5px; color: #94a3b8; line-height: 1.5; }
.openai-error { grid-column: 1 / -1; font-size: 12.5px; color: #ef4444; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 8px 12px; }
.openai-actions { grid-column: 1 / -1; margin-top: 4px; }
.openai-tip {
  grid-column: 1 / -1; display: flex; align-items: flex-start; gap: 8px;
  font-size: 12px; color: #64748b; line-height: 1.6;
  background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 10px 12px;
}
.openai-tip i { color: #7c3aed; margin-top: 2px; flex-shrink: 0; }

/* 糖尿病预测信息 */
.health-head { margin-top: 24px; }
.health-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px 20px; }
.health-item { display: flex; flex-direction: column; gap: 5px; }
.health-item .label { font-size: 12px; color: #94a3b8; }
.health-item .value { font-size: 13.5px; color: #1e293b; }
.health-go-btn { margin-top: 16px; }

.health-risk {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  font-size: 13px;
}
.health-risk.risk {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}
.health-risk.diag {
  background: #f5f3ff;
  border: 1px solid #ddd6fe;
}
.health-risk.empty {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}
.risk-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.risk-head i {
  font-size: 14px;
}
.health-risk.risk .risk-head i,
.health-risk.risk .risk-title { color: #2563eb; }
.health-risk.diag .risk-head i,
.health-risk.diag .risk-title { color: #7c3aed; }
.health-risk.empty .risk-head i,
.health-risk.empty .risk-title { color: #64748b; }
.risk-title {
  font-weight: 600;
  font-size: 14px;
}
.risk-score {
  margin-left: auto;
  font-weight: 700;
  font-size: 14px;
  color: #1e3a5f;
}
.risk-text {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}
.health-edit-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px 18px; }
.health-field { display: flex; flex-direction: column; gap: 6px; }
.health-field.full { grid-column: 1 / -1; }
.health-field label { font-size: 12.5px; color: #475569; }
.health-field .seg-group { display: flex; gap: 8px; }
.health-field .seg {
  flex: 1; padding: 8px 0; border: 1px solid #e2e8f0; border-radius: 8px;
  background: #f8fafc; color: #64748b; font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.health-field .seg.on { background: #eff6ff; border-color: #2563eb; color: #2563eb; font-weight: 600; }

.predict-input { display: flex; align-items: center; gap: 6px; }
.predict-input .text-input { flex: 1; min-width: 0; }
.predicted-tag {
  flex-shrink: 0;
  font-style: normal;
  font-size: 11px;
  color: #7c3aed;
  background: #ede9fe;
  padding: 2px 6px;
  border-radius: 6px;
  white-space: nowrap;
}

/* 我的方案 */
.scheme-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.scheme-tabs button {
  padding: 7px 20px; border: 1px solid #e2e8f0; border-radius: 18px;
  background: #f8fafc; color: #475569; font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.scheme-tabs button.active { background: #2563eb; border-color: #2563eb; color: #fff; }
.scheme-name { font-size: 16px; font-weight: 700; color: #1e3a5f; }
.scheme-desc { font-size: 12.5px; color: #94a3b8; margin: 4px 0 14px; }
.scheme-list { display: flex; flex-direction: column; gap: 8px; }
.scheme-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 14px; background: #f8fafc; border-radius: 10px;
  border: 1px solid #eef2f7;
}
.scheme-item i { color: #cbd5e1; font-size: 16px; }
.scheme-item i.done { color: #16a34a; }
.scheme-time { font-size: 12px; color: #94a3b8; width: 110px; flex-shrink: 0; }
.scheme-content { font-size: 13.5px; color: #334155; flex: 1; }

/* 加载 / 空态 */
.loading-view { text-align: center; padding: 50px 30px; color: #64748b; font-size: 13px; }
.loader { display: flex; justify-content: center; gap: 6px; margin-bottom: 12px; }
.loader span {
  width: 8px; height: 8px; border-radius: 50%; background: #2563eb;
  animation: bounce 1.2s infinite ease-in-out;
}
.loader span:nth-child(2) { animation-delay: 0.15s; }
.loader span:nth-child(3) { animation-delay: 0.3s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }
.empty-view { text-align: center; padding: 50px 30px; color: #94a3b8; }
.empty-icon { font-size: 40px; color: #cbd5e1; margin-bottom: 12px; }
.empty-view p { font-size: 13.5px; margin-bottom: 16px; }

/* 我的建议 */
.advice-group { margin-bottom: 18px; }
/* 分组标题：主题色文字 + 彩色圆形图标底 */
.group-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 14px; font-weight: 700; margin-bottom: 12px;
  cursor: pointer; user-select: none;
}
.group-title i {
  width: 24px; height: 24px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 12px; color: #fff;
}
/* 分组折叠箭头（不受上面圆形图标样式影响） */
.group-title .group-arrow {
  width: auto; height: auto; border-radius: 0; background: transparent;
  margin-left: auto; font-size: 12px; color: #cbd5e1;
  transition: transform 0.2s ease;
}
.group-title .group-arrow.up { transform: rotate(180deg); }
.advice-group.eat .group-title { color: #15803d; }
.advice-group.eat .group-title i { background: #16a34a; }
.advice-group.sport .group-title { color: #b45309; }
.advice-group.sport .group-title i { background: #d97706; }
.advice-group.daily .group-title { color: #1d4ed8; }
.advice-group.daily .group-title i { background: #2563eb; }
.advice-group.pop .group-title { color: #6d28d9; }
.advice-group.pop .group-title i { background: #7c3aed; }

.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.advice-tag {
  padding: 6px 14px; border-radius: 16px; font-size: 12.5px;
  background: #eff6ff; color: #2563eb; border: 1px solid #dbeafe;
}
.advice-tag.eat { background: #f0fdf4; color: #16a34a; border-color: #dcfce7; }
.advice-tag.sport { background: #fffbeb; color: #d97706; border-color: #fef3c7; }
.advice-cards { display: grid; grid-template-columns: 1fr; gap: 10px; }
/* 建议卡片：左侧彩色边框条区分不同类别 */
.advice-card {
  padding: 12px 14px 12px 16px; background: #f8fafc; border-radius: 10px;
  border: 1px solid #eef2f7;
  border-left-width: 4px;
  border-left-color: #94a3b8;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.advice-card:hover { transform: translateY(-1px); box-shadow: 0 3px 10px rgba(31, 45, 61, 0.08); }
/* 各分组卡片主题色（背景 + 左侧色条） */
.advice-card.eat { background: #f0fdf4; border-color: #dcfce7; border-left-color: #16a34a; }
.advice-card.sport { background: #fffbeb; border-color: #fef3c7; border-left-color: #d97706; }
.advice-card.daily { background: #eff6ff; border-color: #dbeafe; border-left-color: #2563eb; }
.advice-card.pop { background: #f5f3ff; border-color: #ede9fe; border-left-color: #7c3aed; }
/* 各分组卡片标题主题色 */
.advice-card.eat .card-title-sm { color: #15803d; }
.advice-card.sport .card-title-sm { color: #b45309; }
.advice-card.daily .card-title-sm { color: #1d4ed8; }
.advice-card.pop .card-title-sm { color: #6d28d9; }
.card-title-sm { display: flex; align-items: center; font-size: 13px; font-weight: 600; color: #1e293b; margin-bottom: 6px; }
/* 建议序号徽章：类别主题色 */
.advice-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 4px; border-radius: 9px;
  margin-right: 6px; font-size: 11px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}
.advice-badge.eat { background: #16a34a; }
.advice-badge.sport { background: #d97706; }
.advice-badge.daily { background: #2563eb; }
.advice-badge.pop { background: #7c3aed; }
.card-text { font-size: 12.5px; color: #64748b; line-height: 1.7; }
.card-text :deep(p) { margin: 0 0 6px; }
.card-text :deep(p:last-child) { margin-bottom: 0; }
.card-text :deep(h1),
.card-text :deep(h2),
.card-text :deep(h3),
.card-text :deep(h4) { margin: 10px 0 5px; font-size: 13.5px; color: #1e3a5f; }
.card-text :deep(ul),
.card-text :deep(ol) { margin: 4px 0 6px; padding-left: 16px; }
.card-text :deep(li) { margin: 2px 0; }
.card-text :deep(hr) { margin: 8px 0; border: none; border-top: 1px dashed #d0d7de; }
.card-text :deep(code.md-inline-code) { background: #eef2f7; padding: 1px 4px; border-radius: 4px; font-size: 11.5px; }

/* 打卡记录 */
.punch-stats { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-box {
  flex: 1; text-align: center; padding: 16px 10px;
  background: #f8fafc; border-radius: 12px; border: 1px solid #eef2f7;
}
.stat-num { font-size: 26px; font-weight: 700; color: #2563eb; }
.stat-num.warn { color: #f59e0b; }
.stat-num.success { color: #16a34a; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.record-list { display: flex; flex-direction: column; gap: 8px; }
.record-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 14px; background: #f8fafc; border-radius: 10px;
  border: 1px solid #eef2f7;
}
.record-icon { font-size: 16px; width: 20px; text-align: center; }
.record-info { flex: 1; min-width: 0; }
.record-type { font-size: 13.5px; color: #334155; font-weight: 500; }
.record-date { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.status-badge {
  padding: 4px 12px; border-radius: 14px; font-size: 12px;
  background: #fef2f2; color: #dc2626; white-space: nowrap;
}
.status-badge.ok { background: #f0fdf4; color: #16a34a; }

/* ========== 我的资讯（收藏） ========== */
.fav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
}
.fav-item:hover {
  border-color: #dbeafe;
  background: #eff6ff;
}
.fav-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #2563eb;
  font-size: 16px;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(31, 45, 61, 0.08);
}
.fav-info {
  flex: 1;
  min-width: 0;
}
.fav-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}
.fav-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11.5px;
  color: #94a3b8;
}
.fav-cat {
  color: #2563eb;
  background: #eff6ff;
  padding: 1px 8px;
  border-radius: 8px;
}
.fav-remove {
  padding: 6px 12px;
  border: 1px solid #fde68a;
  border-radius: 16px;
  background: #fffbeb;
  color: #b45309;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.fav-remove:hover {
  background: #fef3c7;
}
.empty-view {
  text-align: center;
  padding: 48px 20px;
  color: #94a3b8;
}
.empty-view .empty-icon {
  font-size: 38px;
  color: #cbd5e1;
  margin-bottom: 12px;
}
.empty-view p {
  margin: 0 0 18px;
  font-size: 13px;
}
.loading-view {
  text-align: center;
  padding: 48px 20px;
  color: #94a3b8;
}

/* 收藏详情弹窗 */
.fav-detail-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}
.fav-detail-dialog {
  width: 620px;
  max-width: 92vw;
  max-height: 80vh;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.3);
}
.fav-detail-dialog .detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eef2f7;
}
.fav-detail-dialog .detail-head h3 {
  margin: 0;
  font-size: 16px;
  color: #1e3a5f;
}
.fav-detail-dialog .detail-head h3 i {
  color: #2563eb;
  margin-right: 6px;
}
.fav-detail-dialog .close-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
.fav-detail-dialog .close-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}
.fav-detail-dialog .detail-tags {
  padding: 14px 20px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.fav-detail-dialog .detail-tags .tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 12px;
  font-size: 12px;
}
.fav-detail-dialog .detail-content {
  flex: 1;
  padding: 14px 20px 18px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.9;
  color: #334155;
}
.fav-detail-dialog .detail-content :deep(p) {
  margin: 0 0 12px;
}
.fav-detail-dialog .detail-foot {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #eef2f7;
  background: #f8fafc;
}
.fav-detail-dialog .unfav-btn {
  padding: 9px 24px;
  border: 1px solid #fde68a;
  border-radius: 20px;
  background: #fffbeb;
  color: #b45309;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.2s;
}
.fav-detail-dialog .unfav-btn:hover {
  background: #fef3c7;
}

/* ====== 打卡：头部操作按钮 ====== */
.head-actions {
  display: flex;
  gap: 8px;
}

.add-btn {
  background: #eff6ff;
  border-color: #dbeafe;
  color: #2563eb;
}

.add-btn:hover {
  background: #dbeafe;
}

/* ====== 打卡：备注 + 删除按钮 ====== */
.record-message {
  margin-left: 8px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.record-del {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #cbd5e1;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  transition: background 0.2s, color 0.2s;
}

.record-del:hover {
  background: #fee2e2;
  color: #ef4444;
}

.record-del:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 打卡列表「完成」按钮（未完成记录显示） */
.complete-btn {
  padding: 5px 12px; border: none; border-radius: 14px;
  background: #ecfdf5; color: #059669; font-size: 12px;
  cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
  white-space: nowrap; transition: background 0.15s ease;
}
.complete-btn:hover { background: #d1fae5; }
.complete-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ====== 新增打卡弹窗 ====== */
.punch-tip {
  display: flex; align-items: flex-start; gap: 6px;
  font-size: 12px; color: #94a3b8; line-height: 1.6;
  padding: 8px 10px; background: #f8fafc; border-radius: 8px;
}
.punch-tip i { color: #2563eb; margin-top: 2px; }

.punch-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.punch-dialog {
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.3);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.punch-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eef2f7;
}

.punch-dialog-head h3 {
  margin: 0;
  font-size: 16px;
  color: #1e3a5f;
}

.punch-dialog-head h3 i {
  color: #2563eb;
  margin-right: 6px;
}

.punch-dialog-head .close-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.punch-dialog-head .close-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.punch-dialog-body {
  padding: 18px 20px;
  overflow-y: auto;
}

.punch-form-item {
  margin-bottom: 16px;
}

.punch-form-item > label {
  display: block;
  font-size: 13px;
  color: #475569;
  margin-bottom: 8px;
  font-weight: 500;
}

.punch-type-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.punch-type-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.punch-type-btn:hover {
  border-color: #93c5fd;
}

.punch-type-btn.on {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
  font-weight: 600;
}

.punch-type-btn i {
  font-size: 14px;
}

.punch-form-item .seg-group {
  display: flex;
  gap: 10px;
}

.punch-form-item .seg {
  flex: 1;
  padding: 9px 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.punch-form-item .seg.on {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
  font-weight: 600;
}

.punch-dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #eef2f7;
  background: #f8fafc;
}

.punch-dialog-foot .cancel-btn {
  padding: 9px 22px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 13.5px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.punch-dialog-foot .cancel-btn:hover {
  background: #f1f5f9;
}

.punch-dialog-foot .confirm-btn {
  padding: 9px 24px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-size: 13.5px;
  font-family: inherit;
  cursor: pointer;
  transition: opacity 0.2s;
}

.punch-dialog-foot .confirm-btn:hover {
  opacity: 0.9;
}

.punch-dialog-foot .confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .panel-card {
    padding: 16px;
  }
  .pwd-form {
    flex-direction: column;
  }
  .pwd-form .text-input {
    width: 100%;
    min-width: 0;
  }
  .punch-stats {
    gap: 8px;
  }
  .stat-box {
    padding: 12px 8px;
  }
  .stat-num {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .card-head {
    flex-wrap: wrap;
    gap: 8px;
  }
  .profile-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px 14px;
  }
  .health-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px 14px;
  }
  .health-edit-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .advice-cards {
    grid-template-columns: 1fr;
  }
  .scheme-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 4px;
  }
  .scheme-tabs button {
    flex-shrink: 0;
  }
  .scheme-item {
    align-items: flex-start;
  }
  .scheme-time {
    width: auto;
    min-width: 80px;
    font-size: 11px;
  }
  .avatar-line {
    flex-direction: column;
    align-items: flex-start;
  }
  .punch-dialog-mask {
    padding: 12px;
  }
  .punch-dialog {
    width: 100%;
    max-height: 85vh;
  }
  .punch-dialog-foot .cancel-btn,
  .punch-dialog-foot .confirm-btn {
    flex: 1;
    text-align: center;
  }
  .health-risk {
    padding: 14px;
  }
}
</style>
