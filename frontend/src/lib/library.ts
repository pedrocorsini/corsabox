import { useCallback, useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { api, pageInfo, type LibraryEntry, type LibraryStatus } from './api'

const PAGE_SIZE = 24

export function useLibraryList(status: LibraryStatus) {
  const { token } = useAuth()
  const [items, setItems] = useState<LibraryEntry[]>([])
  const [total, setTotal] = useState<number | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const page = useRef(0)
  const busy = useRef(false)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    api.library
      .list(token, status, 0, PAGE_SIZE)
      .then((d) => {
        if (cancelled) return
        const info = pageInfo(d)
        page.current = 0
        setItems(d.content)
        setTotal(info.total)
        setHasMore(info.hasMore)
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

  const loadMore = useCallback(async () => {
    if (!token || busy.current || !hasMore) return
    busy.current = true
    setLoadingMore(true)
    setLoadError(false)
    try {
      const d = await api.library.list(token, status, page.current + 1, PAGE_SIZE)
      page.current += 1
      setItems((prev) => [...prev, ...d.content])
      setHasMore(pageInfo(d).hasMore)
    } catch {
      setLoadError(true)
    } finally {
      busy.current = false
      setLoadingMore(false)
    }
  }, [token, status, hasMore])

  return { items, total, hasMore, loading, loadingMore, error, loadError, retry, loadMore }
}