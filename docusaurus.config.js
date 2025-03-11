// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const {themes} = require('prism-react-renderer/dist/index');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Apktool',
  tagline: 'A tool for reverse engineering Android apk files',
  favicon: 'img/favicon.ico',
  url: 'https://apktool.org',
  baseUrl: '/',
  projectName: 'Apktool',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },
  plugins: [
    [
      '@docusaurus/plugin-client-redirects',
      {
        redirects: [
          {
            to: '/docs/install',
            from: '/install/',
          },
          {
            to: '/docs/build',
            from: '/build/',
          },
          {
            to: '/docs/the-basics/intro',
            from: '/documentation/',
          },
          {
            to: '/blog/googlecode-shutdown',
            from: '/googlecode/',
          },
          {
            to: '/blog',
            from: '/changes/',
          },
          {
            to: '/docs/meta/contributing',
            from: '/contribute/',
          },
        ],
      },
    ],
  ],
  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/iBotPeaches/Apktool/tree/docs/',
        },
        blog: {
          showReadingTime: false,
          postsPerPage: 2,
          blogSidebarCount: 'ALL',
          editUrl: 'https://github.com/iBotPeaches/Apktool/tree/docs/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],
  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/social-card.png',
      navbar: {
        title: 'Apktool',
        logo: {
          alt: 'Apktool Logo',
          src: 'img/logo.png',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Docs',
          },
          {to: '/docs/install', label: 'Install', position: 'left'},
          {to: '/blog', label: 'Releases', position: 'left'},
          {
            href: 'https://bitbucket.org/iBotPeaches/apktool/downloads/apktool_2.11.1.jar',
            label: 'Download 2.11.1',
            position: 'right',
          },
          {
            href: 'https://github.com/iBotPeaches/Apktool',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'How to Install',
                to: '/docs/install',
              },
              {
                label: 'How to Build',
                to: 'docs/build',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'GitHub Discussions',
                href: 'https://github.com/iBotPeaches/Apktool/discussions',
              },
              {
                label: 'Stack Overflow',
                href: 'https://stackoverflow.com/questions/tagged/apktool',
              },
              {
                label: 'IRC (Libera)',
                href: 'https://web.libera.chat/#apktool',
              },
            ],
          },
          {
            title: 'Maintainer',
            items: [
              {
                label: 'Twitter',
                href: 'https://twitter.com/iBotPeaches',
              },
              {
                label: 'Mastodon',
                href: 'https://infosec.exchange/@iBotPeaches',
              },
              {
                label: 'Blog',
                href: 'https://connortumbleson.com/tag/apktool/',
              },
            ],
          },
        ],
        copyright: `Built with Docusaurus.`,
      },
      prism: {
        theme: themes.github,
        darkTheme: themes.dracula,
        additionalLanguages: ['java', 'smali', 'bash', 'javascript'],
      },
      algolia: {
        appId: 'P3KZHU1SCW',
        apiKey: '10bf8c8c0768d8404b2bc87d75ec8c1b',
        indexName: 'apktool',
        contextualSearch: true,
        externalUrlRegex: 'apktool\\.org',
        searchParameters: {},
        searchPagePath: 'search',
      },
    }),
};

module.exports = config;
