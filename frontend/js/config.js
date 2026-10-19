// =====================================================================
//  ConfeitaSys - configuração do FRONTEND
//  O frontend (estas telas) é separado do backend (API Spring Boot).
// =====================================================================

// Endereço da API (backend). Padrão: mesmo computador, porta 8080.
const API_URL = (location.protocol === 'http:' || location.protocol === 'https:')
  ? `${location.protocol}//${location.hostname}:8080`
  : 'http://localhost:8080';

// Token de login guardado no navegador (vale 12 horas no servidor).
const Sessao = {
  token() { try { return localStorage.getItem('confeitasys_token'); } catch { return null; } },
  salvar(t) { try { localStorage.setItem('confeitasys_token', t); } catch { /* sem armazenamento */ } },
  limpar() { try { localStorage.removeItem('confeitasys_token'); } catch { /* ok */ } }
};

// Mensagem quando o backend não responde.
const MSG_SEM_API = `Não foi possível falar com a API em ${API_URL}. Verifique se o backend (Spring Boot) está rodando.`;
