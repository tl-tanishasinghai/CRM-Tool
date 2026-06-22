'use client'

import React, { createContext, useContext, useState, ReactNode } from 'react'
import './Toast.css'

type ToastType = 'info' | 'success' | 'error'

type Toast = {
  id: number
  message: string
  type: ToastType
  dismissible: boolean
}

interface ToastContextType {
  addToast: (message: string, type?: ToastType, duration?: number | null, dismissible?: boolean) => void
  removeToast: (id: number) => void
  clearToasts: () => void
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export const useToast = () => {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}

interface ToastProviderProps {
  children: ReactNode
}

export const ToastProvider = ({ children }: ToastProviderProps) => {
  const [toasts, setToasts] = useState<Toast[]>([])

  const addToast = (
    message: string,
    type: ToastType = 'info',
    duration: number | null = 3000,
    dismissible: boolean = false
  ) => {
    const id = Date.now()
    const newToast: Toast = { id, message, type, dismissible }
    setToasts((prev) => [...prev, newToast])

    if (!dismissible) {
      const timeoutDuration = duration ?? 3000
      setTimeout(() => {
        setToasts((prev) => prev.filter((toast) => toast.id !== id))
      }, timeoutDuration)
    }
  }

  const removeToast = (id: number) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id))
  }

  const clearToasts = () => {
    setToasts([])
  }

  return (
    <ToastContext.Provider value={{ addToast, removeToast, clearToasts }}>
      {children}
      <div className="toast-container">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast ${toast.type}`}>
            <span className="toast-message">{toast.message}</span>
            {toast.dismissible && (
              <button className="toast-close" onClick={() => removeToast(toast.id)}>
                ×
              </button>
            )}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
