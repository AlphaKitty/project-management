/**
 * 通用剪贴板工具
 * 支持多种复制方式的降级策略，确保在各种环境下都能正常工作
 */

/**
 * 使用传统的 document.execCommand 方式复制文本
 */
function fallbackCopyTextToClipboard(text: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const textArea = document.createElement('textarea')
    textArea.value = text
    
    // 避免滚动到底部
    textArea.style.top = '0'
    textArea.style.left = '0'
    textArea.style.position = 'fixed'
    textArea.style.opacity = '0'
    textArea.style.pointerEvents = 'none'
    textArea.style.zIndex = '-1'
    
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()
    
    try {
      const successful = document.execCommand('copy')
      document.body.removeChild(textArea)
      
      if (successful) {
        resolve()
      } else {
        reject(new Error('执行复制命令失败'))
      }
    } catch (err) {
      document.body.removeChild(textArea)
      reject(err)
    }
  })
}

/**
 * 通用复制文本到剪贴板
 * 优先使用现代 Clipboard API，失败时降级到传统方法
 */
export async function copyToClipboard(text: string): Promise<void> {
  if (!text) {
    throw new Error('复制内容不能为空')
  }

  // 优先使用现代 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text)
      return
    } catch (err) {
      console.warn('Clipboard API 失败，尝试降级方案:', err)
      // 降级到传统方法
    }
  }

  // 降级到传统的 execCommand 方法
  try {
    await fallbackCopyTextToClipboard(text)
  } catch (err) {
    console.error('所有复制方法都失败了:', err)
    throw new Error('复制失败，请手动选择并复制内容')
  }
}

/**
 * 从DOM元素复制文本内容
 */
export async function copyElementText(element: HTMLElement): Promise<void> {
  if (!element) {
    throw new Error('目标元素不存在')
  }

  const text = element.innerText || element.textContent || ''
  if (!text.trim()) {
    throw new Error('元素内容为空')
  }

  await copyToClipboard(text)
}

/**
 * 检查剪贴板API是否可用
 */
export function isClipboardSupported(): boolean {
  return !!(navigator.clipboard && window.isSecureContext) || document.queryCommandSupported?.('copy')
}

/**
 * 获取剪贴板支持信息
 */
export function getClipboardInfo(): {
  modern: boolean
  fallback: boolean
  supported: boolean
} {
  const modern = !!(navigator.clipboard && window.isSecureContext)
  const fallback = document.queryCommandSupported?.('copy') || false
  
  return {
    modern,
    fallback,
    supported: modern || fallback
  }
}

/*
使用示例：

// 1. 复制简单文本
try {
  await copyToClipboard('要复制的文本')
  Message.success('复制成功')
} catch (error) {
  Message.error('复制失败')
}

// 2. 复制DOM元素内容
try {
  const element = document.getElementById('content')
  await copyElementText(element)
  Message.success('内容已复制')
} catch (error) {
  Message.error('复制失败')
}

// 3. 检查剪贴板支持
if (isClipboardSupported()) {
  // 显示复制按钮
} else {
  // 隐藏复制按钮或显示提示
}

// 4. 获取详细支持信息
const info = getClipboardInfo()
console.log('现代API支持:', info.modern)
console.log('降级方案支持:', info.fallback)
console.log('整体支持:', info.supported)
*/ 