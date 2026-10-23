# Nitrosend SDK first-send report

- Environment: Linux, Node v24.19.0, @nitrosend/sdk 0.11.7, TypeScript 7.0.2. Used the same existing account and ready hosted sender as my CLI run; authentication came only from NITROSEND_API_KEY.
- Exact install: `npm install --no-audit --no-fund @nitrosend/sdk@0.11.7 typescript @types/node`. Started at 2026-10-23 00:26:43 UTC; npm reported five packages added in 19 seconds.
- Strict verification: `./node_modules/.bin/tsc --strict --noEmit --module esnext --moduleResolution bundler --target es2022 --types node send.ts` exited 0 without diagnostics. The accompanying 11-line script uses `new Nitrosend(key)` and `messages.send(params, idempotencyKey)`.
- Execution: `node send.ts` exited 0. Message 5170664 queued at 00:27:34.007 UTC; script completed at 00:27:36.549 UTC. Install-start to queue: 0.85 minutes. Nitrosend confirmed arrival at 00:27:38 UTC and issued https://runx.ai/r/920280da87051d48e9502ef94431d87f051cd8f2eeb4b8b32a982075926a92a5.
- Slowest measured step: npm installation, 19 seconds. Existing sender setup avoided repeating browser onboarding. One actual message went to the designated proof inbox with a stable idempotency key.
- No SDK defect found. Checked strict compilation, environment-key narrowing, required plain-string idempotency argument, and the successful send against https://github.com/nitrosend/typescript-sdk/blob/main/README.md and https://docs.nitrosend.com/cli/reference. Node emitted MODULE_TYPELESS_PACKAGE_JSON because the generated local package lacked `type: module`; execution succeeded.
- Improvement: include `type: module` in the standalone TypeScript quickstart's package example to prevent that warning for `node send.ts` users.
