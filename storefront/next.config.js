/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://nginx/:path*',
      },
    ];
  },
  images: {
    remotePatterns: [
      {
        hostname: 'api.yas.local',
      },
    ],
  },
};

module.exports = nextConfig;
