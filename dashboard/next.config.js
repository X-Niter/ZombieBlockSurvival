/** @type {import('next').NextConfig} */
const repoName = process.env.GITHUB_REPOSITORY?.split('/')[1] || '';
const isGithubPages = process.env.NODE_ENV === 'production' && repoName;

/**
 * Dynamically configure Next.js for GitHub Pages deployments
 */
const nextConfig = {
  output: 'export',
  reactStrictMode: true,
  trailingSlash: true,
  distDir: 'out',
  images: { unoptimized: true },
  assetPrefix: isGithubPages ? `/${repoName}/` : '',
  basePath: isGithubPages ? `/${repoName}` : '',
}

module.exports = nextConfig;