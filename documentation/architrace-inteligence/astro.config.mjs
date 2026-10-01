// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import mermaid from 'astro-mermaid';

export default defineConfig({
  site: 'https://architrace-intelligence.github.io',
  base: '/Architrace',
  integrations: [
    mermaid({ theme: 'neutral', autoTheme: true }),
    starlight({
      title: 'Architrace Intelligence',
      description: 'Runtime architecture intelligence for distributed systems.',
      sidebar: [
        {
          label: 'Overview',
          items: [
            { label: 'Home', slug: '' },
            { label: 'Getting Started', slug: 'getting-started' },
            { label: 'Architecture', slug: 'architecture' }
          ]
        },
        {
          label: 'Guides',
          items: [
            { label: 'Local Development', slug: 'guides/local-development' },
            { label: 'Docker Demo', slug: 'guides/docker-demo' }
          ]
        },
        {
          label: 'Reference',
          items: [
            { label: 'CLI Commands', slug: 'reference/cli' },
            { label: 'Configuration', slug: 'reference/configuration' },
            { label: 'gRPC Contract', slug: 'reference/grpc-contract' },
            { label: 'Modules', slug: 'reference/modules' }
          ]
        },
        {
          label: 'Project',
          items: [
            { label: 'Current state', slug: 'project/state' },
            { label: 'Requirements', slug: 'project/requirements' },
            { label: 'Progress', slug: 'project/progress' },
            { label: 'Contributing', slug: 'project/contributing' },
            { label: 'GitHub setup', slug: 'project/github-access' },
            { label: 'Features', autogenerate: { directory: 'project/features' } },
            { label: 'Decisions', autogenerate: { directory: 'project/adr' } }
          ]
        }
      ]
    })
  ]
});
