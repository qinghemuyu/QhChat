// 为不同环境提供全局变量
if (typeof global === 'undefined') {
  window.global = window;
}

if (typeof process === 'undefined') {
  window.process = { env: {} };
}

// 确保 Buffer 在浏览器环境中可用
if (typeof window !== 'undefined' && typeof window.Buffer === 'undefined') {
  window.Buffer = {
    isBuffer: () => false
  };
} 