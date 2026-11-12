# Sanji Gram — 9 Rules (check karne ke niyam)

Har code change, commit aur build se pehle ye 9 rules follow karo. Koi shortcut nahi.
Ye rules build #14 aur #15 ke failures se bane hain — galat import, galat return type,
aur assume kiya hua field — teeno dobara nahi hone chahiye.

1. **Pehle 2 bar check karo** — bug ya requirement ko asli code aur logs me verify karo, guess mat karo.
2. **List banao** — kya badlega, kaunsi files, kaunse functions — pehle likh lo.
3. **Sahi code likho** — stock Telegram patterns follow karo, assumed API mat use karo.
4. **Har import verify karo** — har import ka package asli file me check karo (galat package = build fail).
5. **Har type, field aur method verify karo** — return type, field, method signature asli code me grep karke check karo.
6. **Koi assumed API nahi** — jo bhi method ya field use kar rahe ho, uska proof is codebase me hona chahiye.
7. **Poora diff dobara padho** — saari changed files ka diff ek bar phir se, line by line.
8. **Phir se 2 bar check karo** — javac parse check + diff dobara; dono pass hona chahiye.
9. **Sirf sahi ho to aage badho** — tabhi commit + push + build karo. Build fail ho to step 1 se dobara shuru karo, shortcut mat lo.
