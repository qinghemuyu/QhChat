import { format } from 'date-fns'

export const formatTime = (timestamp) => {
  return format(new Date(timestamp), 'HH:mm')
} 