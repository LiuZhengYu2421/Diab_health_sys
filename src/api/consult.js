/**
 * 健康资讯接口封装
 * ============================================
 * 真实模式：请求 SpringBoot 文章接口（articles 表）。
 *   文章由管理员在管理后台生成（支持 Dify「健康资讯」工作流 AI 生成），
 *   普通用户在此查看资讯并收藏（收藏落库 article_collections）。
 * Mock 模式：本地模拟 articles 表数据 + localStorage 收藏（consultFavorites.js）。
 *
 * 接口约定：
 *  GET  /api/articles              资讯列表（分页 + 分类 + 搜索）
 *  GET  /api/articles/categories   资讯分类
 *  GET  /api/articles/{id}         资讯详情（浏览量 +1）
 *  GET  /api/articles/favorites    我的收藏
 *  POST /api/articles/{id}/favorite  收藏/取消收藏（切换）
 */
import request from './request'
import {
  getConsultFavorites,
  isConsultFavorite,
  addConsultFavorite,
  removeConsultFavorite
} from '@/utils/consultFavorites'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

function mockDelay(ms = 500) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 分类元信息（Mock 数据与 UI 图标映射） */
export const CATEGORY_MAP = {
  eat: { name: '饮食指导', icon: 'fa-solid fa-utensils', cls: 'cat-green' },
  sport: { name: '运动指南', icon: 'fa-solid fa-person-walking', cls: 'cat-blue' },
  daily: { name: '日常习惯', icon: 'fa-solid fa-bed', cls: 'cat-orange' },
  popularization: {
    name: '糖尿病科普',
    icon: 'fa-solid fa-book-medical',
    cls: 'cat-purple'
  }
}

/** Mock 数据源：模拟 articles 表内容（与管理员 AI 生成资讯的展示效果一致） */
const FALLBACK_TAGS = {
  eat: [
    {
      title: '糖尿病饮食指南：主食粗细搭配技巧',
      content:
        '<p>建议用糙米、燕麦、杂豆替代部分精米白面，延缓餐后血糖上升；每餐先吃蔬菜再吃主食能进一步平稳血糖。</p>'
    },
    {
      title: '低GI 食物清单：适合糖友的 10 种主食',
      content:
        '<p>燕麦、糙米、藜麦、红薯、玉米、杂粮馒头、全麦面包、山药、芋头、绿豆都是常见低 GI 选项。</p>'
    },
    {
      title: '控糖早餐怎么吃？营养师教你搭配',
      content:
        '<p>一份优质早餐应包含蛋白质（如鸡蛋 / 无糖豆浆）、复合碳水（全麦面包 / 杂粮粥）和蔬菜，比例约为 2:1:1。</p>'
    }
  ],
  sport: [
    {
      title: '科学运动控糖：每周 150 分钟有氧计划',
      content:
        '<p>每周至少 5 天、每天 30 分钟中等强度运动（心率 110-130 次/分），快走、骑车、游泳都是不错的选择。</p>'
    },
    {
      title: '散步也能降血糖？餐后 20 分钟快走实验',
      content:
        '<p>餐后 20 分钟开始的中等强度步行，可显著降低餐后 2 小时血糖约 1.5-2 mmol/L，简单易行。</p>'
    }
  ],
  daily: [
    {
      title: '糖友日常注意事项',
      content:
        '<p>规律作息、足部护理、情绪管理、定期复诊都是日常控糖的关键环节，坚持小习惯，收获大健康。</p>'
    }
  ],
  popularization: [
    {
      title: '认识 2 型糖尿病',
      content:
        '<p>2 型糖尿病是最常见的糖尿病类型，与胰岛素抵抗及胰岛功能减退相关，科学管理可有效延缓并发症发生。</p>'
    },
    {
      title: '糖化血红蛋白是什么',
      content:
        '<p>糖化血红蛋白反映近 2-3 个月的平均血糖水平，是评估长期控糖效果的金标准。</p>'
    }
  ]
}

/** 构建 Mock 列表（展平分类数据为文章对象） */
function buildMockList() {
  const list = []
  Object.keys(CATEGORY_MAP).forEach((key) => {
    const meta = CATEGORY_MAP[key]
    ;(FALLBACK_TAGS[key] || []).forEach((item, i) => {
      list.push({
        articleId: list.length + 1,
        title: item.title,
        category: meta.name,
        content: item.content,
        views: 0,
        faved: isConsultFavorite(item.title)
      })
    })
  })
  return list
}

/** Mock：资讯分类列表 */
function mockCategories() {
  return mockDelay(200).then(() => Object.values(CATEGORY_MAP).map((c) => c.name))
}

/** Mock：资讯分页列表 */
function mockArticles(page, pageSize, category) {
  let list = buildMockList()
  if (category && category !== '全部') list = list.filter((a) => a.category === category)
  const total = list.length
  const start = (page - 1) * pageSize
  return mockDelay(400).then(() => ({
    list: list.slice(start, start + pageSize),
    total,
    page,
    pageSize
  }))
}

/** 资讯列表（分页 + 分类） */
export function getArticles({ page = 1, pageSize = 10, category = '' } = {}) {
  if (USE_MOCK) return mockArticles(page, pageSize, category)
  return request.get('/articles', { params: { page, pageSize, category } })
}

/** 资讯分类列表 */
export function getArticleCategories() {
  if (USE_MOCK) return mockCategories()
  return request.get('/articles/categories')
}

/** 资讯详情（浏览量 +1） */
export function getArticleDetail(articleId) {
  if (USE_MOCK) {
    const item = buildMockList().find((a) => a.articleId === articleId) || {}
    return mockDelay(300).then(() => ({ ...item, tags: [] }))
  }
  return request.get(`/articles/${articleId}`)
}

/** 我的收藏列表（用于列表收藏状态标记） */
export function getMyFavorites() {
  if (USE_MOCK) return mockDelay(300).then(() => getConsultFavorites())
  return request.get('/articles/favorites')
}

/**
 * 收藏/取消收藏（切换）
 * @returns {Promise<boolean>} 切换后的收藏状态
 */
export function toggleFavoriteArticle(article) {
  if (USE_MOCK) {
    if (isConsultFavorite(article.title)) {
      removeConsultFavorite(article.title)
      return mockDelay(300).then(() => false)
    }
    addConsultFavorite({
      title: article.title,
      content: article.content || '',
      tags: article.tags || [],
      category: article.category || ''
    })
    return mockDelay(300).then(() => true)
  }
  return request.post(`/articles/${article.articleId}/favorite`)
}
