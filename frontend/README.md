## FaultLine Frontend (Angular)

This is the Seismic Risk Dashboard UI for FaultLine, built with **Angular 15**.

### Scripts

- `npm install` – install dependencies
- `npm start` or `npm run dev` – start the Angular dev server at **http://localhost:4200**
- `npm run build` – production build (output in `dist/faultline-frontend`)

The frontend expects the backend API at **http://localhost:8080/api**. To use a different base URL, edit `src/app/services/api.service.ts` and change the `apiBase` property (or use Angular environments for different builds).
