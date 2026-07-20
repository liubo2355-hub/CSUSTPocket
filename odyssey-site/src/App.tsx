import { useCallback, useEffect, useRef, useState } from 'react'
import { motion, useMotionValueEvent, useScroll } from 'framer-motion'
import { ArrowUpRight, ChevronDown, Crosshair, Radio, Signal } from 'lucide-react'
import './index.css'

const lerp = (from: number, to: number, amount: number) => from + (to - from) * amount

const sections = [
  {
    eyebrow: '01 / ORIGIN',
    title: 'Beyond the familiar.',
    copy: 'A quiet voyage through the dark. Every frame is a signal from somewhere further out.',
    align: 'left',
  },
  {
    eyebrow: '02 / DRIFT',
    title: 'Weightless by design.',
    copy: 'Release the pull of the ordinary. Move with the silence between the stars.',
    align: 'right',
  },
  {
    eyebrow: '03 / HORIZON',
    title: 'The edge is a beginning.',
    copy: 'Keep scrolling to move through the archive. The next horizon is already in motion.',
    align: 'left',
  },
] as const

function App() {
  const videoRef = useRef<HTMLVideoElement>(null)
  const targetProgress = useRef(0)
  const rafRef = useRef<number | null>(null)
  const [videoReady, setVideoReady] = useState(false)
  const [signalSent, setSignalSent] = useState(false)
  const { scrollYProgress } = useScroll()

  useMotionValueEvent(scrollYProgress, 'change', (latest) => {
    targetProgress.current = latest
  })

  useEffect(() => {
    const tick = () => {
      const video = videoRef.current
      if (video && videoReady && Number.isFinite(video.duration) && video.duration > 0) {
        const destination = targetProgress.current * video.duration
        const nextTime = lerp(video.currentTime, destination, 0.09)
        if (Math.abs(destination - nextTime) > 0.001) {
          video.currentTime = nextTime
        }
      }
      rafRef.current = requestAnimationFrame(tick)
    }

    rafRef.current = requestAnimationFrame(tick)
    return () => {
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current)
    }
  }, [videoReady])

  const resetToTop = useCallback(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  const transmitSignal = () => {
    setSignalSent(true)
    window.setTimeout(() => setSignalSent(false), 2200)
  }

  return (
    <main className="odyssey-shell">
      <a className="skip-link" href="#mission">
        SKIP TO MISSION
      </a>

      <div className="visual-layer" aria-hidden="true">
        <video
          ref={videoRef}
          className="space-video"
          src="/space-bg.mp4"
          muted
          playsInline
          preload="auto"
          onLoadedMetadata={(event) => {
            event.currentTarget.pause()
            event.currentTarget.currentTime = 0
            setVideoReady(true)
          }}
        />
        <div className="video-dim" />
        <div className="grid-overlay" />
        <div className="edge-vignette" />
      </div>

      <header className="topbar">
        <a className="brand" href="#mission" aria-label="Odyssey home">
          <span className="brand-mark"><Crosshair size={18} strokeWidth={1.5} /></span>
          <span>ODYSSEY</span>
        </a>
        <div className="topbar-meta">
          <span className="status-dot" />
          <span>LIVE ARCHIVE / 008</span>
        </div>
      </header>

      <div className="scroll-marker" aria-hidden="true">
        <span>SCROLL TO NAVIGATE</span>
        <ChevronDown size={16} strokeWidth={1.5} />
      </div>

      <div className="story" id="mission">
        <section className="hero-panel story-panel">
          <motion.div
            className="hero-copy"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 1, ease: [0.22, 1, 0.36, 1] }}
          >
            <p className="eyebrow"><span className="eyebrow-rule" /> DEEP SPACE TRANSMISSION</p>
            <h1>WE ARE<br /><em>STILL MOVING.</em></h1>
            <p className="hero-intro">A visual record of the human impulse to go further, carried on a current of light.</p>
            <div className="hero-signature">
              <span>MISSION / 08:00</span>
              <span>STATUS / IN TRANSIT</span>
            </div>
          </motion.div>
        </section>

        {sections.map((section) => (
          <section className={`story-panel story-panel--${section.align}`} key={section.eyebrow}>
            <motion.div
              className="story-copy"
              initial={{ opacity: 0, y: 38 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ amount: 0.5, once: false }}
              transition={{ duration: 0.75, ease: [0.22, 1, 0.36, 1] }}
            >
              <p className="eyebrow"><span className="eyebrow-rule" /> {section.eyebrow}</p>
              <h2>{section.title}</h2>
              <p>{section.copy}</p>
            </motion.div>
          </section>
        ))}

        <section className="story-panel story-panel--right final-panel" id="signal">
          <motion.div
            className="story-copy final-copy"
            initial={{ opacity: 0, x: 32 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ amount: 0.45, once: false }}
            transition={{ duration: 0.85, ease: [0.22, 1, 0.36, 1] }}
          >
            <p className="eyebrow eyebrow--yellow"><span className="eyebrow-rule" /> 04 / TRANSMISSION</p>
            <h2>ETERNAL<br />ODYSSEY</h2>
            <p>In sync with the stars, sailing to the edge of time and space.</p>
            <motion.button
              className={`signal-button ${signalSent ? 'signal-button--sent' : ''}`}
              type="button"
              onClick={transmitSignal}
              whileHover={{ x: 5 }}
              whileTap={{ scale: 0.97 }}
            >
              <span>{signalSent ? 'SIGNAL RECEIVED' : 'TRANSMIT SIGNAL'}</span>
              <ArrowUpRight size={17} strokeWidth={1.5} />
            </motion.button>
          </motion.div>
        </section>

        <footer className="site-footer">
          <div>
            <span className="footer-kicker">CREATIVE SYSTEM / 2026</span>
            <strong>JUEZHI DESIGN</strong>
          </div>
          <button className="back-top" type="button" onClick={resetToTop}>
            RETURN TO ORIGIN <ArrowUpRight size={15} strokeWidth={1.5} />
          </button>
        </footer>
      </div>

      <footer className="statusbar" aria-label="System status">
        <div className="statusbar-item"><Radio size={14} strokeWidth={1.5} /> SYS: ONLINE</div>
        <div className="progress-track" aria-hidden="true">
          <motion.div className="progress-fill" style={{ scaleX: scrollYProgress }} />
        </div>
        <div className="statusbar-item statusbar-coords"><Signal size={14} strokeWidth={1.5} /> COORD: 42° 21' 11.2&quot; N</div>
      </footer>
    </main>
  )
}

export default App
