<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00d4ff,100:2575fc&height=200&section=header&text=FaultLine&fontSize=45&fontColor=ffffff"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-00d4ff?style=for-the-badge&logo=openjdk&logoColor=black" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-000000?style=for-the-badge&logo=spring&logoColor=00d4ff" />
  <img src="https://img.shields.io/badge/Angular-15-00d4ff?style=for-the-badge&logo=angular&logoColor=black" />
  <img src="https://img.shields.io/badge/LLM-Ready-000000?style=for-the-badge&logo=openai&logoColor=00d4ff" />
</p>

## 🔎 What is FaultLine?
**FaultLine** is a "Seismic Sensor" for codebases. It is a full-stack engineering tool that identifies hidden technical debt by merging a repository's **behavioral history** with its **structural complexity.** While most tools just look at the code, FaultLine looks at the *story* of the code to find the "Faults" before they lead to production outages.

---

## 🛠️ How it Works (The Core Logic)
FaultLine calculates a **Risk Score (0-100)** by analyzing two distinct data dimensions:

1.  **Commit Churn (Behavioral Analysis):** Using **JGit**, it traverses the repository's history to find "Hotspots"—files that change constantly or are touched by too many developers.
2.  **AST Metrics (Structural Analysis):** Using **Eclipse JDT**, it parses Java source code into an *Abstract Syntax Tree* to measure deep metrics like method length, class nesting, and parameter bloat.
3.  **AI Interpretation:** Once the data is gathered, an **LLM** generates natural language insights, telling you *exactly* why a file is risky and how to refactor it.

---

## 🌟 Why is this helpful?
* **For Team Leads:** Instantly identify which modules are becoming "God Objects" and slowing down the team.
* **For Developers:** Get AI-assisted refactoring suggestions that go beyond simple "linting" rules.
* **For Onboarding:** Help new engineers visualize the most complex parts of a legacy codebase through the **Seismic Risk Dashboard.**

---

## 🎯 Key Technical Highlights
- 🔄 **Real-time Sync**: Async processing with **SSE (Server-Sent Events)** updates so you can watch the analysis happen live.
- 🧠 **Context-Aware AI**: It doesn't just flag long methods; it uses repo-wide metrics to give actionable recommendations.
- 🐳 **Portable Architecture**: Fully Dockerized and demo-ready with an internal H2 database.

---

## 🎨 Visuals — Risk Legend
| Level | Description | Status |
| :--- | :--- | :--- |
| 🟢 **LOW** | Stable code with low complexity. | Safe to scale. |
| 🟡 **MEDIUM** | Moderate churn or rising complexity. | Monitor for debt. |
| 🔴 **HIGH** | "The FaultLine"—Frequent changes + Messy logic. | **Refactor ASAP.** |

---

## ⚡ Quickstart (Demo Mode)

1️⃣ **Initialize Demo Repo:**
```bash
cd sample-repos/demo-repo && bash init-repo.sh
