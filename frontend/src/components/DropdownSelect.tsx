import { useRef } from 'react'
import { ChevronDown } from 'lucide-react'

export const dropdownMenuClass = 'category-menu-in absolute top-full z-50 mt-2 max-h-72 w-full max-w-[min(90vw,20rem)] overflow-y-auto rounded-none border border-white/10 bg-surface p-2 shadow-xl'

export function DropdownSelect<T extends string>({ label, value, options, onChange, name }: {
  label: string
  name?: string
  value: T
  options: readonly { id: T; label: string }[]
  onChange: (value: T) => void
}) {
  const menu = useRef<HTMLDetailsElement>(null)

  return (
    <details ref={menu} name={name} className="genre-filter group relative w-full"
      onKeyDown={(event) => {
        if (event.key === 'Escape' && menu.current?.open) {
          menu.current.open = false
          menu.current.querySelector('summary')?.focus()
        }
      }}>
      <summary aria-label={`${label}: ${options.find((option) => option.id === value)?.label}`}
        className="flex h-9 w-full select-none cursor-pointer list-none items-center justify-between gap-3 bg-surface px-3 text-sm text-white outline-none focus:outline-2 focus:outline-accent">
        <span className="truncate">{options.find((option) => option.id === value)?.label}</span>
        <ChevronDown size={16} aria-hidden="true" className="shrink-0 transition-transform duration-200 group-open:rotate-180 motion-reduce:transition-none" />
      </summary>
      <div role="group" aria-label={label} className={`${dropdownMenuClass} left-0`}>
        {options.map((option) => (
          <button key={option.id} type="button" aria-pressed={option.id === value}
            onClick={() => {
              onChange(option.id)
              if (menu.current) {
                menu.current.open = false
                menu.current.querySelector('summary')?.focus()
              }
            }}
            className={`block w-full px-3 py-2 text-left text-sm transition-colors hover:bg-line hover:text-white ${option.id === value ? 'bg-line text-white' : 'text-muted'}`}>
            {option.label}
          </button>
        ))}
      </div>
    </details>
  )
}

