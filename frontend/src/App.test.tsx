import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('앱', () => {
    it('첫 화면 → MOA 제목', () => {
        render(<App />)

        expect(screen.getByRole('heading', { name: 'MOA' })).toBeInTheDocument()
    })
})