import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { request } from './client.ts'

describe('API 클라이언트', () => {
    it('200 → 본문 JSON을 돌려줌', async () => {
        server.use(
            http.get('/api/stores', () => HttpResponse.json([{ id: 1, name: '모아 카페' }])),
        )

        const result = await request('GET', '/api/stores')

        expect(result).toEqual([{ id: 1, name: '모아 카페' }])
    })

    it('POST → 본문을 JSON으로 보냄', async () => {
        let received: unknown
        server.use(
            http.post('/api/stores', async ({ request: req }) => {
                received = await req.json()
                return HttpResponse.json({ id: 1 }, { status: 201 })
            }),
        )

        await request('POST', '/api/stores', { name: '모아 카페' })

        expect(received).toEqual({ name: '모아 카페' })
    })

    it('POST → Content-Type: application/json 헤더를 붙임', async () => {
        let contentType: string | null = null
        server.use(
            http.post('/api/stores', ({ request: req }) => {
                contentType = req.headers.get('Content-Type')
                return HttpResponse.json({ id: 1 }, { status: 201 })
            }),
        )

        await request('POST', '/api/stores', { name: '모아 카페' })

        expect(contentType).toBe('application/json')
    })

    it('409 → ApiError(409, 코드)', async () => {
        server.use(
            http.post('/api/stores', () =>
                HttpResponse.json({ code: 'STORE_ALREADY_EXISTS', message: '이미 있음' }, { status: 409 }),
            ),
        )

        const call = request('POST', '/api/stores', { name: '모아 카페' })

        await expect(call).rejects.toMatchObject({ status: 409, code: 'STORE_ALREADY_EXISTS' })
    })
})