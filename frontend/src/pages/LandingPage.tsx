import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { Brand } from '../components/Brand'
import { LiveDiary } from '../components/LiveDiary'
import { PosterSkeleton, PosterStrip } from '../components/PosterStrip'
import { KIND_LABEL, itemKey, useTrending } from '../lib/catalog'
import { Wordmark } from '../components/Wordmark'
import { useScrollEffects } from '../lib/motion'

const GROUPS = [
  { name: 'Buscar', summary: 'Encontre qualquer título e veja os detalhes antes de decidir.', items: [
    ['Filmes', 'Longas de todas as épocas e países.'],
    ['Séries', 'Títulos em exibição e séries já encerradas.'],
    ['Animes', 'Animação japonesa no mesmo lugar que o resto.'],
    ['Pôsteres e sinopses', 'A capa e o resumo de cada título.'],
    ['Elenco e direção', 'Quem atua e quem dirige.'],
  ] },
  { name: 'Registrar', summary: 'Seu histórico de tudo o que já passou pela tela.', items: [
    ['Marcar como assistido', 'Um toque e o título entra no seu histórico.'],
    ['Ver seu histórico', 'O que você já viu, do mais recente ao mais antigo.'],
    ['Corrigir um registro', 'Marcou errado? Edite ou remova.'],
  ] },
  { name: 'Guardar', summary: 'A fila do que você ainda quer ver.', items: [
    ['Criar a watchlist', 'Uma lista só sua, sem prazo para acabar.'],
    ['Adicionar um título', 'Achou algo bom? Guarde para depois.'],
    ['Passar para assistidos', 'Viu? Mova da fila para o histórico.'],
    ['Tirar da lista', 'Mudou de ideia? Remova.'],
  ] },
] as const

const delay = (i: number) => ({ transitionDelay: `${i * 80}ms` })

function PrimaryCta({ className = '', onPaper = false }: { className?: string; onPaper?: boolean }) {
  const { token } = useAuth()
  return (
    <Link to={token ? '/catalogo' : '/register'}
      className={`inline-flex h-14 items-center justify-center px-8 font-semibold transition active:scale-[0.97] ${onPaper ? 'bg-ink text-paper hover:bg-ash' : 'bg-accent text-ink hover:brightness-105'} ${className}`}>
      {token ? 'Abrir catálogo' : 'Criar conta'}
    </Link>
  )
}

/* Botões da primeira tela, sobre o campo escuro: os dois com preenchimento forte */
function HeroActions() {
  const { token } = useAuth()
  const base = 'inline-flex h-11 items-center justify-center px-4 text-[15px] font-semibold transition active:scale-[0.97] md:h-12 md:px-6'
  if (token) return <Link to="/catalogo" className={`${base} bg-accent text-ink hover:brightness-105`}>Abrir catálogo</Link>
  return (
    <>
      <Link to="/login" className={`${base} bg-paper text-ink hover:bg-paper-3`}>Entrar</Link>
      <Link to="/register" className={`${base} bg-accent text-ink hover:brightness-105`}>Criar conta</Link>
    </>
  )
}

export default function LandingPage() {
  const [hint, setHint] = useState<string>(GROUPS[0].items[0][1])
  const { items, loading } = useTrending()
  const [activeKey, setActiveKey] = useState<string | null>(null)
  const paused = useRef(false)
  useScrollEffects()

  useEffect(() => {
    if (items.length === 0) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const t = setInterval(() => {
      if (paused.current) return
      setActiveKey((key) => {
        const current = items.findIndex((i) => itemKey(i) === key)
        return itemKey(items[((current < 0 ? 0 : current) + 1) % items.length])
      })
    }, 5000)
    return () => clearInterval(t)
  }, [items])

  const featured = items.find((i) => itemKey(i) === activeKey) ?? items[0]

  return (
    <>
      <main className="relative z-10">
        {/* Hero: a fita de pôsteres com os títulos em alta da TMDB */}
        <section data-field="ink" className="relative flex min-h-svh flex-col overflow-hidden bg-bg py-5 text-white md:py-6">
          {featured?.backdropUrl && (
            <>
              <img key={itemKey(featured)} src={featured.backdropUrl} alt="" className="fade-in absolute inset-0 h-full w-full object-cover" />
              <div className="absolute inset-0 bg-bg/75" />
            </>
          )}
          <header className="relative flex items-center justify-between px-5 md:px-8">
            <Link to="/"><Brand /></Link>
            <nav className="flex items-center gap-2 md:gap-3">
              <a href="#recursos" className="mr-3 hidden text-[15px] hover:underline md:inline">Recursos</a>
              <a href="#como-funciona" className="mr-4 hidden text-[15px] hover:underline md:inline">Como funciona</a>
              <HeroActions />
            </nav>
          </header>

          <div className="relative grid gap-8 px-5 pt-12 md:grid-cols-12 md:gap-x-6 md:px-8 md:pt-[8svh]">
            <div className="md:col-span-8">
              <h1 className="mask display-n text-[9.5vw] md:text-[6.6vw]"><span>Tudo o que<br />você assistiu.</span></h1>
              <p data-reveal style={{ transitionDelay: '500ms' }} className="mt-6 max-w-[44ch] text-lg md:text-xl">
                CorsaBox é o seu diário de filmes, séries e animes. Registre o que você viu e guarde o que quer ver.
              </p>
            </div>
            {featured && (
              <div key={itemKey(featured)} className="fade-in md:col-span-3 md:col-start-10 md:self-end">
                <p className="wide text-2xl md:text-3xl">{featured.title}</p>
                <p className="mt-1 text-[15px] text-muted">
                  Em alta agora · {KIND_LABEL[featured.kind]}{featured.year ? ` · ${featured.year}` : ''}
                </p>
              </div>
            )}
          </div>

          <div className="relative mt-10 h-[40svh] md:mt-auto md:h-[44svh]">
            {loading ? (
              <PosterSkeleton />
            ) : (
              <PosterStrip items={items} activeKey={activeKey ?? itemKey(featured)}
                onActive={(key) => { paused.current = true; setActiveKey(key) }}
                onLeave={() => { paused.current = false }} />
            )}
          </div>
        </section>

        {/* Recursos: nomes em colunas, descrição no canto ao passar o mouse */}
        <section id="recursos" data-field="paper" className="bg-paper text-ink">
          <div className="mx-auto max-w-[1600px] px-5 py-[18vh] md:px-8 md:py-[20vh]">
            <div className="grid gap-12 md:grid-cols-12 md:gap-x-6">
              <h2 data-reveal className="text-[28px] font-semibold leading-tight md:col-span-12 md:mb-12">
                O que você faz na CorsaBox
              </h2>
              {GROUPS.map((g, gi) => (
                <div key={g.name} data-reveal style={delay(gi)}
                  className={`md:row-start-2 md:col-span-3 ${['md:col-start-1', 'md:col-start-4', 'md:col-start-7'][gi]}`}>
                  <p className="text-[1.0625rem] font-medium md:text-lg">{g.name}</p>
                  <ul className="mt-6 space-y-1 text-lg leading-tight text-ash">
                    {g.items.map(([name, desc]) => (
                      <li key={name} tabIndex={0} onMouseEnter={() => setHint(desc)} onFocus={() => setHint(desc)}
                        className="cursor-default transition-colors hover:text-ink focus-visible:text-ink">
                        {name}
                      </li>
                    ))}
                  </ul>
                  <p className="mt-6 text-ash md:hidden">{g.summary}</p>
                </div>
              ))}
              <p aria-live="polite" className="hidden text-lg md:col-span-3 md:col-start-10 md:row-start-2 md:block">{hint}</p>
            </div>
          </div>
        </section>

        {/* Como funciona: coluna de leitura e o diário vivo do outro lado */}
        <section id="como-funciona" data-field="ink" className="bg-bg text-white">
          <div className="mx-auto grid max-w-[1600px] gap-16 px-5 py-[18vh] md:grid-cols-12 md:gap-x-6 md:px-8 md:py-[20vh]">
            <div className="md:col-span-5 md:col-start-2">
              <h2 data-reveal className="text-[28px] font-semibold leading-tight md:text-[32px]">Do cadastro ao primeiro registro</h2>
              <div className="mt-12 max-w-[560px] space-y-14 text-[1.0625rem] leading-[1.6] md:text-lg">
                <p data-reveal><strong className="font-semibold">Para começar,</strong> crie sua conta com email e senha. O login com Google chega depois.</p>
                <p data-reveal><strong className="font-semibold">No catálogo,</strong> procure um filme, uma série ou um anime e abra a página do título para ver sinopse, elenco e direção.</p>
                <p data-reveal><strong className="font-semibold">Daqui em diante,</strong> marque o que você viu e guarde o que quer ver. As duas listas ficam separadas e você muda um título de lugar quando quiser.</p>
              </div>
            </div>
            <div data-reveal className="self-start md:col-span-5 md:col-start-8 md:sticky md:top-24">
              <LiveDiary />
            </div>
          </div>
        </section>

        {/* Fechamento: a ideia de novo, invertida */}
        <section data-field="paper" className="flex min-h-[80svh] flex-col justify-between bg-paper px-5 pb-10 pt-16 text-ink md:min-h-[90svh] md:px-8">
          <h2 data-reveal className="display-n text-[11.5vw] md:text-[9vw]">Seu diário<br />começa aqui.</h2>
          <div className="grid gap-8 md:grid-cols-12 md:items-end md:gap-x-6">
            <p data-reveal className="text-xl md:col-span-5">Crie sua conta e comece a registrar o que você assiste.</p>
            <div data-reveal style={delay(1)} className="md:col-span-3 md:col-start-10 md:justify-self-end">
              <PrimaryCta onPaper className="w-full md:w-auto" />
            </div>
          </div>
        </section>
      </main>

      {/* Rodapé: revelado por baixo da página */}
      <footer data-field="ink" className="sticky bottom-0 z-0 flex min-h-svh flex-col justify-between bg-bg px-5 pb-5 pt-10 text-white md:px-8">
        <div className="grid grid-cols-2 gap-x-6 gap-y-10 text-[15px] md:grid-cols-4">
          <div className="space-y-1 text-muted"><p className="text-white">Explorar</p><a href="#recursos" className="block hover:text-white">Recursos</a><a href="#como-funciona" className="block hover:text-white">Como funciona</a></div>
          <div className="space-y-1 text-muted"><p className="text-white">Conta</p><Link to="/login" className="block hover:text-white">Entrar</Link><Link to="/register" className="block hover:text-white">Criar conta</Link></div>
          <div className="space-y-1 text-muted"><p className="text-white">Dados</p><p>Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB.</p></div>
          <div className="space-y-1 text-muted"><p className="text-white">Status</p><p>Em construção. Cadastro e login já funcionam.</p></div>
        </div>
        <div>
          <Wordmark />
          <p className="mt-5 text-[13px] text-muted">© 2026 CorsaBox</p>
        </div>
      </footer>
    </>
  )
}