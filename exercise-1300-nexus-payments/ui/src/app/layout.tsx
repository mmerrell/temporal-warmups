import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Nexus Payments Dashboard',
  description: 'Cross-team payment compliance via Temporal Nexus',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-gray-950 text-gray-100 min-h-screen">
        <nav className="bg-gray-900 border-b border-gray-800 px-6 py-4">
          <div className="max-w-7xl mx-auto flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center font-bold text-sm">N</div>
              <h1 className="text-lg font-semibold">Nexus Payments Dashboard</h1>
            </div>
            <div className="flex gap-6">
              <a href="/" className="text-gray-300 hover:text-white transition">Transactions</a>
              <a href="/approvals" className="text-gray-300 hover:text-white transition">Approvals</a>
              <a href="http://localhost:8233" target="_blank" className="text-indigo-400 hover:text-indigo-300 transition">Temporal UI</a>
            </div>
          </div>
        </nav>
        <main className="max-w-7xl mx-auto px-6 py-8">
          {children}
        </main>
      </body>
    </html>
  )
}
