/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // [ADDED] Docker 최적화: standalone 모드
  // node_modules 없이 .next/standalone 디렉터리만으로 실행 가능
  // 결과 이미지 크기: ~1GB → ~200MB 수준으로 감소
  output: "standalone",
};

module.exports = nextConfig;