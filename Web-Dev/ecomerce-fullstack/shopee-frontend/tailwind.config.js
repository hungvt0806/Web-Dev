/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        display: ['"Syne"', 'sans-serif'],
        body:    ['"DM Sans"', 'sans-serif'],
        mono:    ['"DM Mono"', 'monospace'],
      },
      colors: {
        ink:   { DEFAULT: '#0A0A0A', 50: '#1a1a1a', 100: '#111' },
        ash:   { DEFAULT: '#F5F3EE', dark: '#E8E4DC' },
        ember: { DEFAULT: '#E8441A', light: '#FF6B45', dark: '#C23210' },
        sage:  { DEFAULT: '#4A7C6A', light: '#6BA391' },
        gold:  { DEFAULT: '#D4A843', light: '#EAC46B' },
      },
      animation: {
        'fade-up':    'fadeUp 0.5s ease forwards',
        'fade-in':    'fadeIn 0.3s ease forwards',
        'slide-in':   'slideIn 0.4s cubic-bezier(0.16,1,0.3,1) forwards',
        'shimmer':    'shimmer 1.5s infinite',
        'spin-slow':  'spin 3s linear infinite',
      },
      keyframes: {
        fadeUp:   { from: { opacity: 0, transform: 'translateY(16px)' }, to: { opacity: 1, transform: 'none' } },
        fadeIn:   { from: { opacity: 0 }, to: { opacity: 1 } },
        slideIn:  { from: { opacity: 0, transform: 'translateX(-20px)' }, to: { opacity: 1, transform: 'none' } },
        shimmer:  { '0%': { backgroundPosition: '-200% 0' }, '100%': { backgroundPosition: '200% 0' } },
      },
      backgroundImage: {
        'noise': "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.04'/%3E%3C/svg%3E\")",
      },
      boxShadow: {
        'lift': '0 4px 24px -4px rgba(0,0,0,0.12), 0 2px 8px -2px rgba(0,0,0,0.08)',
        'hard': '4px 4px 0px 0px #0A0A0A',
        'ember': '0 8px 32px -8px rgba(232,68,26,0.4)',
      },
    },
  },
  plugins: [],
}
