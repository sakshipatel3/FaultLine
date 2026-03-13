<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&color=0:6a11cb,100:2575fc&height=200&section=header&text=FaultLine&fontSize=45&fontColor=ffffff"/>
</p>

<div>
  <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="build"/>
  <img src="https://img.shields.io/badge/Java-17-brightgreen" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=spring" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Angular-15-red?logo=angular" alt="Angular"/>
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?logo=docker" alt="Docker"/>
  <img src="https://img.shields.io/badge/LLM-ready-purple" alt="LLM"/>
</div>

## ✨ What it is
A fun, fast startup to analyze Git repositories for risk: combines commit churn (JGit) + Java AST metrics (Eclipse JDT) to highlight high‑risk files and generate AI-assisted engineering insights.

## 🚀 Tech snapshot (one line)
**Java, Spring Boot, JGit, Eclipse JDT, LLM integration, JPA/H2, OpenAPI, Angular, Docker**

## 🎯 Key highlights
- **Risk fusion**: churn + AST → single 0–100 score  
- **AI insights**: configurable prompt → actionable recommendations  
- **Async jobs**: background analysis + SSE progress updates  
- **Demo-ready**: `H2` + sample repo included

## 🎨 Visuals — risk legend
<div>
  <span style="display:inline-block;padding:6px 10px;border-radius:4px;background:#4caf50;color:#fff;margin-right:6px;">LOW</span>
  <span style="display:inline-block;padding:6px 10px;border-radius:4px;background:#ffeb3b;color:#000;margin-right:6px;">MEDIUM</span>
  <span style="display:inline-block;padding:6px 10px;border-radius:4px;background:#f44336;color:#fff;">HIGH</span>
</div>

## ⚡ Quickstart (super short)
1. Initialize demo repo:
   - `cd sample-repos/demo-repo && bash init-repo.sh`
2. Run backend:
   - `cd backend && mvn clean package && java -jar target/repo-risk-analyzer-backend-0.1.0.jar`
3. (Optional) Frontend dev:
   - `cd frontend && npm install && npm start`
4. Analyze sample (curl):
```bash
curl -X POST http://localhost:8080/api/projects/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"file:///ABS/PATH/sample-repos/demo-repo","branch":"main","depth":100}'
