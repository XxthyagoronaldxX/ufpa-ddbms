# TESTE 1: Criar tabela e adicionar usuário.

- Validar se foi replicado para todos os nós.

# TESTE 2: Usar Script para forçar o loadbalancing.

- COMANDO: ab -n 50 -c 50 -p read-payload.json -T application/json http://localhost:8080/api/read
- Validar se os nós estão recebendo requests de acordo com o algoritmo: O nó com menor quantidade de conexões recebe a request. 

# TESTE 3: Testando eleição de coordenador. (Caso o atual seja desligado)

- Validar se o nó com o maior ID foi eleito.

# TESTE 4: Inserir um dado e depois reativar um nó que estava desligado.

- Validar se o nó que estava desligado pega as informações inseridas no periodo que estava desativado.