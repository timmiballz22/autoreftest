import { Nitrosend } from '@nitrosend/sdk';
const key = process.env.NITROSEND_API_KEY;
if (!key) throw new Error('Set NITROSEND_API_KEY');
const ns = new Nitrosend(key);
const result = await ns.messages.send({
  channel: 'email',
  to: 'proof@dogfood.nitrosend.net',
  subject: 'SDK first-send verification',
  html: '<p>One authorized SDK QA test from the FurryFragz account to the Nitrosend proof inbox.</p>',
}, 'furryfragz-sdk-first-send-20261023');
console.log(JSON.stringify({ completedAt: new Date().toISOString(), result }));
