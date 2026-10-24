// frontend/src/lib/library.ts
import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { api, type LibraryEntry, type LibraryStatus } from './api'

const PAGE_SIZE = 24

export function useLibraryList(status: LibraryStatus) {
  const { token } = useAuth()
  const [items, setItems] = useState<LibraryEntry[]>([])
  const [total, setTotal] = useState<number | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const page = useRef(0)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    api.library
      .list(token, status, 0, PAGE_SIZE)
      .then((d) => {
        if (cancelled) return
        page.current = 0
        setItems(d.content)
        setTotal(d.totalElements)
        setHasMore(!d.last)
        setLoading(false)
      })
      .catch(() => {
        if (cancelled) return
        setError(true)
        setLoading(false)
      })
    return () => { cancelled = true }
  }, [token, status, attempt])

  const retry = () => {
    setError(false)
    setLoading(true)
    setAttempt((a) => a + 1)
  }

  const loadMore = async () => {
    if (!token || loadingMore) return
    setLoadingMore(true)
    try {
      const d = await api.library.list(token, status, page.current + 1, PAGE_SIZE)
      page.current += 1
      setItems((prev) => [...prev, ...d.content])
      setHasMore(!d.last)
    } catch {
      setError(true)
    } finally {
      setLoadingMore(false)
    }
  }

  return { items, total, hasMore, loading, loadingMore, error, retry, loadMore }
}