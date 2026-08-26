// 轻量 Markdown 渲染器（安全：先转义 HTML，再解析基础语法）
// 供各页面渲染 AI 回复等 markdown 文本，避免 v-html 直接注入的风险。
// 用法：<div class="markdown-body" v-html="renderMarkdown(text)"></div>

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInline(s) {
  return s
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
    .replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')
    .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
}

/**
 * 将 markdown 文本渲染为安全的 HTML 字符串
 * 支持：标题 #~######（含无空格写法）、粗体、斜体、行内代码、
 *       代码块 ```、无序/有序列表、引用、水平线、链接。
 */
export function renderMarkdown(text) {
  if (!text) return ''
  const safe = escapeHtml(text)
  let out = ''
  let listType = null
  let listItems = []
  let para = []
  let inCode = false
  let codeLines = []

  const flushPara = () => {
    if (para.length) {
      out += `<p>${renderInline(para.join('<br>'))}</p>`
      para = []
    }
  }
  const flushList = () => {
    if (listType) {
      const tag = listType === 'ol' ? 'ol' : 'ul'
      out += `<${tag}>${listItems.map((i) => `<li>${renderInline(i)}</li>`).join('')}</${tag}>`
      listType = null
      listItems = []
    }
  }

  safe.split('\n').forEach((raw) => {
    const line = raw.trim()
    // 代码块开关 ```...```
    if (line.startsWith('```')) {
      if (!inCode) {
        flushPara()
        flushList()
        inCode = true
        codeLines = []
      } else {
        inCode = false
        out += `<pre class="md-code"><code>${codeLines.join('\n')}</code></pre>`
      }
      return
    }
    if (inCode) {
      codeLines.push(line)
      return
    }
    if (!line) {
      flushPara()
      flushList()
      return
    }
    // 标题 # ~ ######（兼容 AI 输出 "###1.xxx" 无空格写法）
    const h = line.match(/^(#{1,6})[ \t]*([^#].+)$/)
    if (h) {
      flushPara()
      flushList()
      out += `<h${h[1].length}>${renderInline(h[2])}</h${h[1].length}>`
      return
    }
    // 水平线 --- / *** / ___
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line)) {
      flushPara()
      flushList()
      out += '<hr>'
      return
    }
    // 列表 - item / * item / 1. item / 1、item（兼容 "-每6个月" 无空格写法）
    const ul = line.match(/^[-*][ \t]*(.+)$/)
    const ol = line.match(/^\d+[.、][ \t]*(?!\d)(.+)$/)
    if (ul || ol) {
      flushPara()
      const type = ol ? 'ol' : 'ul'
      if (listType && listType !== type) flushList()
      listType = type
      listItems.push((ul ? ul[1] : ol[1]).trim())
      return
    }
    // 普通行（相邻行合并为一段，单换行转 <br>）
    flushList()
    para.push(line)
  })
  flushPara()
  flushList()
  return out
}
