## FaultLine Frontend

This is the Seismic Risk Dashboard UI for FaultLine.

### Scripts

- `npm install` – install dependencies
- `npm run dev` – start the frontend on `http://localhost:5173`
- `npm run build` – production build

The frontend expects the backend API to be available at `http://localhost:8080/api` by default. You can override this by setting `VITE_API_BASE_URL` in a `.env` file in this folder.

