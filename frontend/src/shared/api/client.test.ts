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
})