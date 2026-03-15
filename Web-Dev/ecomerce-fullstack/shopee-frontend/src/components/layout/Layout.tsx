import { Outlet } from 'react-router-dom'
import { Navbar } from './Navbar'
import { Footer } from './Footer'
import { CartDrawer } from './CartDrawer'
import { Toaster } from 'react-hot-toast'

export function Layout() {
  return (
    <div className="min-h-screen flex flex-col bg-ash">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
      <CartDrawer />
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#0A0A0A',
            color: '#F5F3EE',
            fontFamily: 'DM Sans, sans-serif',
            fontSize: '14px',
            borderRadius: 0,
          },
          success: { iconTheme: { primary: '#4A7C6A', secondary: '#F5F3EE' } },
          error:   { iconTheme: { primary: '#E8441A', secondary: '#F5F3EE' } },
        }}
      />
    </div>
  )
}
