// Tela de login / primeiro cadastro (RF1)
(function () {
  const form = document.getElementById('form');
  const erro = document.getElementById('erro');
  const botao = document.getElementById('botao');
  const campoConfirmar = document.getElementById('campo-confirmar');
  let primeiroAcesso = false;

  function mostrarErro(msg) {
    erro.textContent = msg;
    erro.classList.remove('oculto');
  }

  API.acesso.status().then(st => {   // API 1.3 situação do sistema
    if (st.logado) { location.href = 'app.html'; return; }
    if (!st.temUsuario) {
      primeiroAcesso = true;
      document.getElementById('subtitulo').textContent = 'Primeiro acesso: crie o usuário do sistema.';
      campoConfirmar.classList.remove('oculto');
      document.getElementById('senha').autocomplete = 'new-password';
      botao.textContent = 'Criar usuário e entrar';
    }
  }).catch(() => mostrarErro(MSG_SEM_API));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    erro.classList.add('oculto');
    const nome = document.getElementById('nome').value.trim();
    const senha = document.getElementById('senha').value;
    if (!nome || !senha) return mostrarErro('Preencha usuário e senha.');
    if (primeiroAcesso) {
      if (senha.length < 6) return mostrarErro('A senha deve ter pelo menos 6 caracteres.');
      if (senha !== document.getElementById('confirmar').value) return mostrarErro('As senhas não conferem.');
    }
    botao.disabled = true;
    try {
      // API 2.3 cadastrar usuário (primeiro acesso)  ou  API 1.1 entrar
      const dados = primeiroAcesso ? await API.usuario.cadastrar(nome, senha) : await API.acesso.login(nome, senha);
      Sessao.salvar(dados.token);
      location.href = 'app.html';
    } catch (ex) {
      mostrarErro(ex.message);
      document.getElementById('senha').select();
    } finally {
      botao.disabled = false;
    }
  });
})();
