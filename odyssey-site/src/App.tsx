import { useCallback, useEffect, useRef, type ReactNode } from 'react'
import { animate, motion, useScroll, useTransform, type MotionValue } from 'framer-motion'
import {
  ArrowUpRight,
  ChevronDown,
  Crosshair,
  Download,
  Radio,
  Signal,
} from 'lucide-react'
import './index.css'

const sections = [
  {
    eyebrow: '01 / CORE FEATURES',
    title: '课表、成绩、考试，一处掌握。',
    copy: '面向长沙理工大学师生，把分散在不同系统中的高频信息收进一个清晰的 Android 应用。',
    details: ['课表查询', '成绩与绩点', '考试安排', '网络课程', '待提交作业'],
    align: 'left',
  },
  {
    eyebrow: '02 / CAMPUS LIFE',
    title: '校园服务与隐私，简单可靠。',
    copy: '空教室、校园地图、校历、电费充值、WebVPN、评教系统与桌面小组件都在这里。学号和密码只用于访问学校业务系统，敏感凭据保存在本地。',
    details: ['宿舍电量', '用电趋势', '校园地图', 'WebVPN', '本地保存'],
    align: 'right',
  },
] as const

function EnteringMotion({
  pageIndex,
  align,
  progress,
  className,
  children,
}: {
  pageIndex: number
  align: 'left' | 'right'
  progress: MotionValue<number>
  className: string
  children: ReactNode
}) {
  const start = (pageIndex - 1) / 3
  const end = pageIndex / 3
  const offset = align === 'left' ? -56 : 56
  const x = useTransform(progress, [start, end], [offset, 0])
  const y = useTransform(progress, [start, end], [12, 0])
  const opacity = useTransform(progress, [start, end], [0, 1])
  const scale = useTransform(progress, [start, end], [0.98, 1])

  return <motion.div className={className} style={{ opacity, x, y, scale }}>{children}</motion.div>
}

function App() {
  const videoRef = useRef<HTMLVideoElement>(null)
  const wheelLocked = useRef(false)
  const touchDragging = useRef(false)
  const { scrollYProgress } = useScroll()

  useEffect(() => {
    window.history.scrollRestoration = 'manual'
    window.scrollTo(0, 0)
  }, [])

  useEffect(() => {
    let activeScrollAnimation: ReturnType<typeof animate> | null = null

    const getNearestPage = (pages: HTMLElement[]) => pages.reduce((bestIndex, page, index) => {
      const bestDistance = Math.abs(window.scrollY - pages[bestIndex].offsetTop)
      const distance = Math.abs(window.scrollY - page.offsetTop)
      return distance < bestDistance ? index : bestIndex
    }, 0)

    const animateToPage = (pageIndex: number) => {
      const pages = Array.from(document.querySelectorAll<HTMLElement>('[data-snap-section]'))
      if (pages.length === 0) return

      const nextPage = Math.max(0, Math.min(pages.length - 1, pageIndex))

      wheelLocked.current = true
      const root = document.documentElement
      const previousSnapType = root.style.scrollSnapType
      root.style.scrollSnapType = 'none'

      activeScrollAnimation = animate(window.scrollY, pages[nextPage].offsetTop, {
        duration: 0.28,
        ease: [0.16, 1, 0.3, 1],
        onUpdate: (value) => window.scrollTo(0, value),
        onComplete: () => {
          root.style.scrollSnapType = previousSnapType
          window.setTimeout(() => {
            wheelLocked.current = false
          }, 80)
        },
      })
    }

    const goToPage = (direction: number, fromPage = -1) => {
      const pages = Array.from(document.querySelectorAll<HTMLElement>('[data-snap-section]'))
      if (pages.length === 0) return
      const currentPage = fromPage >= 0 ? fromPage : getNearestPage(pages)
      const nextPage = Math.max(0, Math.min(pages.length - 1, currentPage + direction))
      if (nextPage === currentPage) {
        animateToPage(currentPage)
        return
      }
      animateToPage(nextPage)
    }

    const handleWheel = (event: WheelEvent) => {
      if (Math.abs(event.deltaY) < 1) return
      event.preventDefault()
      if (wheelLocked.current) return
      goToPage(event.deltaY > 0 ? 1 : -1)
    }

    let touchStartY = 0
    let touchStartX = 0
    let touchStartScrollY = 0
    let touchStartPage = 0
    let touchSnapType = ''
    let verticalTouch = false
    const handleTouchStart = (event: TouchEvent) => {
      const touch = event.changedTouches[0]
      touchStartY = touch.clientY
      touchStartX = touch.clientX
      touchStartScrollY = window.scrollY
      const pages = Array.from(document.querySelectorAll<HTMLElement>('[data-snap-section]'))
      touchStartPage = pages.length > 0 ? getNearestPage(pages) : 0
      touchSnapType = document.documentElement.style.scrollSnapType
      document.documentElement.style.scrollSnapType = 'none'
      touchDragging.current = true
      verticalTouch = false
    }
    const handleTouchMove = (event: TouchEvent) => {
      if (!touchDragging.current) return
      const touch = event.changedTouches[0]
      const deltaY = touchStartY - touch.clientY
      const deltaX = touchStartX - touch.clientX
      if (Math.abs(deltaY) < 8 || Math.abs(deltaY) < Math.abs(deltaX) * 1.15) return
      verticalTouch = true
      event.preventDefault()
      const pages = Array.from(document.querySelectorAll<HTMLElement>('[data-snap-section]'))
      const maxScroll = pages.length > 0 ? pages[pages.length - 1].offsetTop : touchStartScrollY
      const nextScrollY = Math.max(0, Math.min(maxScroll, touchStartScrollY + deltaY))
      window.scrollTo(0, nextScrollY)
    }
    const handleTouchEnd = (event: TouchEvent) => {
      if (!touchDragging.current) return
      touchDragging.current = false
      const touch = event.changedTouches[0]
      const deltaY = touchStartY - touch.clientY
      const deltaX = touchStartX - touch.clientX
      document.documentElement.style.scrollSnapType = touchSnapType
      if (!verticalTouch || Math.abs(deltaY) < 14 || Math.abs(deltaY) < Math.abs(deltaX) * 1.15) {
        animateToPage(touchStartPage)
        return
      }
      event.preventDefault()
      if (wheelLocked.current) return
      goToPage(deltaY > 0 ? 1 : -1, touchStartPage)
    }

    window.addEventListener('wheel', handleWheel, { passive: false })
    window.addEventListener('touchstart', handleTouchStart, { passive: true })
    window.addEventListener('touchmove', handleTouchMove, { passive: false })
    window.addEventListener('touchend', handleTouchEnd, { passive: false })
    return () => {
      activeScrollAnimation?.stop()
      document.documentElement.style.scrollSnapType = ''
      window.removeEventListener('wheel', handleWheel)
      window.removeEventListener('touchstart', handleTouchStart)
      window.removeEventListener('touchmove', handleTouchMove)
      window.removeEventListener('touchend', handleTouchEnd)
    }
  }, [])

  const resetToTop = useCallback(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  return (
    <main className="odyssey-shell">
      <a className="skip-link" href="#mission">
        跳至主要内容
      </a>

      <div className="visual-layer" aria-hidden="true">
        <video
          ref={videoRef}
          className="space-video"
          src={`${import.meta.env.BASE_URL}space-bg.mp4`}
          muted
          playsInline
          preload="auto"
          onLoadedMetadata={(event) => {
            event.currentTarget.pause()
            event.currentTarget.currentTime = 0
          }}
        />
        <div className="video-dim" />
        <div className="grid-overlay" />
        <div className="edge-vignette" />
      </div>

      <header className="topbar">
        <a className="brand" href="#mission" aria-label="返回掌上长理首页">
          <span className="brand-mark"><Crosshair size={18} strokeWidth={1.5} /></span>
          <span>CSUST POCKET</span>
        </a>
        <div className="topbar-meta">
          <span className="status-dot" />
          <span>ANDROID / OPEN SOURCE</span>
        </div>
      </header>

      <div className="scroll-marker" aria-hidden="true">
        <span>SCROLL TO EXPLORE</span>
        <ChevronDown size={16} strokeWidth={1.5} />
      </div>

      <div className="story" id="mission">
        <section className="hero-panel story-panel" data-snap-section>
          <motion.div
            className="hero-copy"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
          >
            <p className="eyebrow"><span className="eyebrow-rule" /> CSUST CAMPUS TOOL / ANDROID</p>
            <h1>掌上长理<br /><em>CSUST POCKET</em></h1>
            <p className="hero-intro">便捷、清晰的校园信息工具。让课表、成绩、考试、课程、电量与校园服务，在一处清晰呈现。</p>
            <div className="hero-signature">
              <span>PLATFORM / ANDROID</span>
              <span>LICENSE / MIT</span>
            </div>
          </motion.div>
        </section>

        {sections.map((section, index) => (
          <section className={`story-panel story-panel--${section.align}`} key={section.eyebrow} data-snap-section>
            <EnteringMotion
              pageIndex={index + 1}
              align={section.align}
              progress={scrollYProgress}
              className="story-copy"
            >
              <p className="eyebrow"><span className="eyebrow-rule" /> {section.eyebrow}</p>
              <h2>{section.title}</h2>
              <p>{section.copy}</p>
              <div className="feature-list">
                {section.details.map((detail) => <span key={detail}>{detail}</span>)}
              </div>
            </EnteringMotion>
          </section>
        ))}

        <section className="story-panel story-panel--right final-panel" id="download" data-snap-section>
          <EnteringMotion
            pageIndex={3}
            align="right"
            progress={scrollYProgress}
            className="story-copy final-copy"
          >
            <p className="eyebrow eyebrow--yellow"><span className="eyebrow-rule" /> 03 / DOWNLOAD</p>
            <h2>下载<br />掌上长理</h2>
            <p>目前提供 Android 测试包，点击按钮即可直接下载到本地。</p>
            <div className="download-actions">
              <motion.a
                className="signal-button"
                href="https://gitee.com/liubo2355-hub/csustpocket/releases/download/v2.0.40/CSUSTPocket_release_v2.0.40.apk"
                download="CSUSTPocket_release_v2.0.40.apk"
                whileHover={{ x: 5 }}
                whileTap={{ scale: 0.97 }}
              >
                <Download size={17} strokeWidth={1.5} />
                <span>直接下载最新 APK</span>
                <ArrowUpRight size={17} strokeWidth={1.5} />
              </motion.a>
            </div>
            <p className="release-note">测试版可更早体验新功能，适合愿意反馈并具备一定 Bug 处理能力的用户。</p>
            <div className="final-open-source">
              <span className="footer-kicker">04 / OPEN SOURCE</span>
              <strong>一起参与掌上长理</strong>
              <p>欢迎通过 Pull Request 参与客户端、服务端、产品与视觉设计工作。</p>
              <nav className="footer-links" aria-label="项目相关链接">
                <a href="https://github.com/liubo2355-hub/CSUSTPocket/blob/main/docs/contribution/%E5%88%86%E6%94%AF%E8%8C%83.md" target="_blank" rel="noreferrer">CONTRIBUTING</a>
                <a href="https://github.com/liubo2355-hub/CSUSTPocket/blob/main/LICENSE" target="_blank" rel="noreferrer">MIT LICENSE</a>
              </nav>
            </div>
          </EnteringMotion>
        </section>

        <footer className="site-footer">
          <motion.div
            className="footer-content"
            initial={{ opacity: 0, y: 72 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ amount: 0.35, once: false }}
            transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
          >
            <span className="footer-kicker">09 / OPEN SOURCE</span>
            <strong>一起参与掌上长理</strong>
            <p>欢迎参与客户端、服务端、产品与视觉设计工作，并通过 Pull Request 贡献代码。</p>
            <div className="tech-stack" aria-label="技术栈">
              {['KOTLIN', 'JETPACK COMPOSE', 'MATERIAL 3', 'ROOM / MMKV', 'RETROFIT / OKHTTP', 'WORKMANAGER', 'GLANCE'].map((item) => <span key={item}>{item}</span>)}
            </div>
            <nav className="footer-links" aria-label="项目相关链接">
              <a href="https://github.com/liubo2355-hub/CSUSTPocket/blob/main/docs/contribution/%E5%88%86%E6%94%AF%E8%A7%84%E8%8C%83.md" target="_blank" rel="noreferrer">CONTRIBUTING</a>
              <a href="https://github.com/liubo2355-hub/CSUSTPocket/blob/main/LICENSE" target="_blank" rel="noreferrer">MIT LICENSE</a>
            </nav>
            <small>掌上长理为非官方校园工具，与长沙理工大学及校内各业务系统不存在隶属或授权关系。数据可能因网络、系统维护或接口调整出现延迟与差异，重要信息请以学校官方渠道为准。</small>
          </motion.div>
          <button className="back-top" type="button" onClick={resetToTop}>
            返回顶部 <ArrowUpRight size={15} strokeWidth={1.5} />
          </button>
        </footer>
      </div>

      <footer className="statusbar" aria-label="系统状态">
        <div className="statusbar-item"><Radio size={14} strokeWidth={1.5} /> SERVICE: READY</div>
        <div className="progress-track" aria-hidden="true">
          <motion.div className="progress-fill" style={{ scaleX: scrollYProgress }} />
        </div>
        <div className="statusbar-item statusbar-coords"><Signal size={14} strokeWidth={1.5} /> CSUST / CHANGSHA</div>
      </footer>
    </main>
  )
}

export default App
