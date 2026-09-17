# Circadian Guard

Ambient light, logged before it becomes a weapon.

---

## Why this exists

Neurocomputing is one of the next advanced technologies expected to follow generative AI. It has several branches, and among them, research is advancing on vision — the most important of the human senses. This is a field we should be watching closely.

Vision and the brain are deeply coupled, and some of that research explores how external stimuli can be used to exert influence. Much of it focuses on image-forming vision — how we recognize objects, faces, and scenes, and how that recognition can be steered.

But vision has another, quieter channel, and it may be the more consequential one.

The retina contains specialized cells called intrinsically photosensitive retinal ganglion cells (ipRGCs). These cells express the photopigment melanopsin and connect directly, via the optic nerve, to the suprachiasmatic nucleus (SCN) in the hypothalamus — the body's master clock. In other words, the optic nerve does not only carry the signal of "what we see." It also carries the signals that govern sleep, wakefulness, and the pupillary reflex.

And here is the part worth pausing on: by controlling the wavelength and timing of light, it is possible to reach in and shift the circadian rhythm — directly, through the optic nerve. This is not hypothetical. It is already used in clinical practice as light therapy.

Consider what that implies. A capability that can shift a person's internal clock is, by its nature, dual-use — the same pathway a clinician uses to reset a patient's sleep cycle can, in other hands, be used to do the opposite. This is why it belongs in the same conversation as security.

Crime has always followed technology. Every new capability — writing, the telegraph, computing, the internet — has sooner or later been turned into a tool for deception, coercion, or profit at someone else's expense. Generative AI has already been used to forge voices, fabricate identities, and automate fraud at scale. There is no reason to believe the next wave will be different.

You can likely see where this is heading. Consider again what ipRGCs do: they couple the outside world's light directly to the body's clock. That means whoever controls the timing and spectrum of that light controls, in part, when a person feels awake, alert, or exhausted — without the person ever noticing. Quiet, universal, and delivered through nothing more than ordinary lighting: this is precisely the kind of lever a malicious actor would reach for.

Given these conditions, the following risks cannot be ruled out:

- **Manipulation of alertness and attention.** Lighting in workplaces, schools, and surveillance spaces can be tuned to specific wavelengths to suppress a group's drowsiness — or, conversely, to fatigue it.
- **Performance degradation through rhythm disruption.** Insomnia- and jetlag-like states can be induced deliberately to dull the judgment of negotiating partners or competitors — a form of "quiet sabotage."
- **Steering attention and dependency.** Combined with digital devices, arousal rhythms can be locked to "specific apps at specific times" — a more powerful variant of social-media addiction.
- **Mass intervention on populations.** Public lighting, television, and smartphone screens — "light that everyone passes through" — enable non-consensual, wide-area physiological entrainment.

In the near future, advances in technology may enable the manipulation of ambient light for malicious intervention in human behavior. We must act preemptively to prevent this. As an initial response, we have released Circadian Guard as a gamma release.

At present, no such threat has been confirmed in this world, and ambient light can be judged to be in a clean state. While this uncontaminated light still exists, we must collect its data.

This data will become a critical means of defense against future optical terrorism.

---

## What Circadian Guard actually does

Circadian Guard is an open-source Android app that records the phone's ambient light (lux) and approximate location to learn the usual brightness of each place, and alerts when the light deviates from that baseline. All collected data is stored only on the device and is never transmitted externally.

## Features

- **Light recording and visualization** (implemented): measures the current brightness (lux) with the ambient light sensor, and displays the current value plus a graph of today's light levels.
- **Location-based baseline learning** (implemented): registers named places (home, work, …) and learns the "usual brightness" of each, online (Welford).
- **Deviation detection and alerts** (implemented): detects when light deviates from the baseline (>3σ and >50 lx) and notifies.

## Permissions

Circadian Guard requests only the minimum necessary permissions.

| Permission | Purpose | Notes |
|---|---|---|
| Ambient light sensor | Measuring brightness | **No permission required** |
| Location (approximate, while in use) | Associating a place | Precise location is not requested |
| Notifications | Deviation alerts | Android 13+ only |

Not requested: camera, microphone, storage, contacts, precise location.

## Privacy

- Data is stored **only on the device** and is never transmitted externally.
- No ads, analytics, or trackers.
- See [PRIVACY.md](PRIVACY.md) for details.

## Build

```bash
./gradlew assembleRelease
```

Distribution channels are currently in preparation (GitHub Releases + Obtainium; F-Droid planned).

## Tech stack

- Kotlin + Jetpack Compose
- Room (local database)
- SensorManager (`TYPE_LIGHT` ambient light sensor)
- LocationManager (`NETWORK_PROVIDER`, coarse — no Google Play Services)
- Welford online statistics (per-place, per-hour, per-weekday baseline)
- minSdk 26 / targetSdk 34

## Disclaimer

- This app is **not a medical device** and is not intended to diagnose, treat, or assess health conditions.
- Measured values vary by device, handling, and environment. Treat them as relative guidance only.
- The framing of "defense against circadian-rhythm manipulation" expresses this app's design intent and does not assert the existence of any specific threat or guarantee safety.

## License

MIT License — see [LICENSE](LICENSE).

## References

- Brainard GC, Hanifin JP, Greeson JM, et al. Action spectrum for melatonin regulation in humans: evidence for a novel circadian photoreceptor. *J Neurosci*. 2001;21(16):6405–6412. doi:10.1523/JNEUROSCI.21-16-06405.2001
- Provencio I, Rodriguez IR, Jiang G, Hayes WP, Moreira EF, Rollag MD. A novel human opsin in the inner retina. *J Neurosci*. 2000;20(2):600–605. doi:10.1523/JNEUROSCI.20-02-00600.2000
- Berson DM, Dunn FA, Takao M. Phototransduction by retinal ganglion cells that set the circadian clock. *Science*. 2002;295(5557):1070–1073. doi:10.1126/science.1067262
- Hattar S, Liao HW, Takao M, Berson DM, Yau KW. Melanopsin-containing retinal ganglion cells: architecture, projections, and intrinsic photosensitivity. *Science*. 2002;295(5557):1065–1070. doi:10.1126/science.1069609
- Cajochen C. Alerting effects of light. *Sleep Med Rev*. 2007;11(6):453–464.
- Lucas RJ, Peirson SN, Berson DM, et al. Measuring and using light in the melanopsin age. *Trends Neurosci*. 2014;37(1):1–9. doi:10.1016/j.tins.2013.10.004
