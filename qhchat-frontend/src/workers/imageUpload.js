// 图片上传 Worker
self.onmessage = async (e) => {
  try {
    const { file, chatCode, sender, apiUrl } = e.data
    const formData = new FormData()
    formData.append('file', file)
    formData.append('chatCode', chatCode)
    formData.append('sender', sender)

    const response = await fetch(`${apiUrl}/api/chat/upload`, {
      method: 'POST',
      body: formData,
      headers: {
        'Accept': 'application/json'
      }
    })

    if (!response.ok) {
      throw new Error(response.statusText || '上传失败')
    }

    const result = await response.json()
    self.postMessage({ success: true, result })
  } catch (error) {
    self.postMessage({ success: false, error: error.message })
  }
} 