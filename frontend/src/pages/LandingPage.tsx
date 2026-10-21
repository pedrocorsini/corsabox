import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { LiveDiary } from '../components/LiveDiary'
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

function PrimaryCta({ className = '' }: { className?: string }) {
  const { token } = useAuth()
  return (
    <Link to={token ? '/catalogo' : '/register'}
      className={`inline-flex h-14 items-center justify-center bg-accent px-8 font-semibold text-ink transition hover:brightness-105 active:translate-y-px ${className}`}>
      {token ? 'Abrir catálogo' : 'Criar conta'}
    </Link>
  )
}

/* Botões da primeira tela: sobre o papel, o principal é tinta cheia e o secundário é um passo de tom mais escuro */
function HeroActions() {
  const { token } = useAuth()
  const base = 'inline-flex h-14 items-center justify-center px-8 font-semibold transition active:translate-y-px'
  if (token)
    return <Link to="/catalogo" className={`${base} bg-ink text-paper hover:bg-ash`}>Abrir catálogo</Link>
  return (
    <div className="flex flex-col gap-3 sm:flex-row">
      <Link to="/register" className={`${base} bg-ink text-paper hover:bg-ash`}>Criar conta</Link>
      <Link to="/login" className={`${base} bg-paper-3 text-ink hover:bg-ink hover:text-paper`}>Entrar</Link>
    </div>
  )
}

export default function LandingPage() {
  const [hint, setHint] = useState<string>(GROUPS[0].items[0][1])
  useScrollEffects()

  return (
    <>
      <main className="relative z-10">
        {/* Hero: o nome é o monumento */}
        <section data-field="paper" className="flex min-h-svh flex-col justify-between bg-paper px-5 pb-5 pt-5 text-ink md:px-8 md:pb-6 md:pt-6">
          <div className="grid gap-10 md:grid-cols-12 md:gap-x-6">
            <nav className="flex justify-end gap-6 text-[15px] md:col-span-4 md:col-start-9 md:row-start-1">
              <a href="#recursos" className="hover:underline">Recursos</a>
              <a href="#como-funciona" className="hover:underline">Como funciona</a>
            </nav>
            <div data-reveal style={{ transitionDelay: '700ms' }} className="md:col-span-5 md:col-start-1 md:row-start-1">
              <p className="max-w-[40ch] text-lg leading-snug md:text-xl">
                CorsaBox é o diário de filmes, séries e animes.
              </p>
              <div className="mt-6"><HeroActions /></div>
            </div>
          </div>
          <div className="mt-10 flex flex-1 flex-col justify-end gap-5 md:gap-8">
            <div className="grid gap-6 md:grid-cols-12 md:items-end md:gap-x-6">
              <div className="mask w-fit md:col-span-5">
                <span>
                  <img src="/favicon.svg" alt="" className="block aspect-square h-[min(40vw,28svh)] w-auto md:h-[min(26vw,34svh)]" />
                </span>
              </div>
              <div className="space-y-2 text-2xl font-semibold leading-tight md:col-span-6 md:col-start-7 md:space-y-3 md:text-[34px]">
                <p data-reveal style={delay(5)}>Registre o que você assistiu.</p>
                <p data-reveal style={delay(6)}>Guarde o que quer ver.</p>
                <p data-reveal style={delay(7)}>Busque filmes, séries e animes.</p>
              </div>
            </div>
            <h1 className="mask" aria-label="CorsaBox, o diário de filmes, séries e animes">
              <span><Wordmark /></span>
            </h1>
          </div>
        </section>

        {/* Recursos: nomes em colunas, descrição no canto ao passar o mouse */}
        <section id="recursos" data-field="ink" className="bg-bg text-white">
          <div className="mx-auto max-w-[1600px] px-5 py-[18vh] md:px-8 md:py-[20vh]">
            <div className="grid gap-12 md:grid-cols-12 md:gap-x-6">
              <h2 data-reveal className="text-[28px] font-semibold leading-tight md:col-span-12 md:mb-12">
                O que você faz na CorsaBox
              </h2>
              {GROUPS.map((g, gi) => (
                <div key={g.name} data-reveal style={delay(gi)}
                  className={`md:row-start-2 md:col-span-3 ${['md:col-start-1', 'md:col-start-4', 'md:col-start-7'][gi]}`}>
                  <p className="text-[1.0625rem] font-medium md:text-lg">{g.name}</p>
                  <ul className="mt-6 space-y-1 text-lg leading-tight text-muted">
                    {g.items.map(([name, desc]) => (
                      <li key={name} tabIndex={0} onMouseEnter={() => setHint(desc)} onFocus={() => setHint(desc)}
                        className="cursor-default transition-colors hover:text-white focus-visible:text-white">
                        {name}
                      </li>
                    ))}
                  </ul>
                  <p className="mt-6 text-muted md:hidden">{g.summary}</p>
                </div>
              ))}
              <p aria-live="polite" className="hidden text-lg md:col-span-3 md:col-start-10 md:row-start-2 md:block">{hint}</p>
            </div>
          </div>
        </section>

        {/* Como funciona: coluna de leitura e o diário vivo do outro lado */}
        <section id="como-funciona" data-field="paper" className="bg-paper text-ink">
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
        <section data-field="ink" className="flex min-h-[80svh] flex-col justify-between bg-bg px-5 pb-10 pt-16 text-white md:min-h-[90svh] md:px-8">
          <h2 data-reveal className="display-n text-[11.5vw] md:text-[9vw]">Seu diário<br />começa aqui.</h2>
          <div className="grid gap-8 md:grid-cols-12 md:items-end md:gap-x-6">
            <p data-reveal className="text-xl md:col-span-5">Crie sua conta e comece a registrar o que você assiste.</p>
            <div data-reveal style={delay(1)} className="md:col-span-3 md:col-start-10 md:justify-self-end">
              <PrimaryCta className="w-full md:w-auto" />
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
