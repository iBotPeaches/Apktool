// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

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
  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/iBotPeaches/Apktool/tree/',
        },
        blog: {
          showReadingTime: true,
          editUrl: 'https://github.com/iBotPeaches/Apktool/tree/',
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
      image: 'img/social-card.jpg',
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
                label: 'Install Guide',
                to: '/docs/install',
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
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
