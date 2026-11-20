import { setupServer } from 'msw/node'

// 테스트용 가짜 서버. 응답 규칙(handler)은 테스트마다 server.use(...)로 넣는다
export const server = setupServer()