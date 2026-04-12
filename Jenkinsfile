pipeline {
	agent any

	tools {
		maven 'maven'
	}

	environment {
		SONAR_TOKEN = credentials('sonar-token')

		URL_CADASTRO = "jdbc:postgresql://db-cadastro:5432/cadastro_db"
		URL_TRANSFERENCIA = "jdbc:postgresql://db-transferencia:5432/transferencia_db"
		URL_NOTIFICACAO = "jdbc:postgresql://db-notificacoes:5432/notificacao_db"
		OIDC_URL = "http://keycloak-estavel:8080/realms/bank-realm"
		KAFKA_URL = "redpanda-estavel:29092"

		DB_USER = "admin"
		DB_PASS = "admin"
		TESTCONTAINERS_RYUK_DISABLED = "true"
		QUARKUS_VAULT_DEVSERVICES_ENABLED = "false"
	}

	stages {
		stage('Build & Test: Cadastro') {
			steps {
				dir('servico-cadastro') {
					sh "mvn clean verify \
                    -Dquarkus.datasource.jdbc.url=${env.URL_CADASTRO} \
                    -Dquarkus.oidc.auth-server-url=${env.OIDC_URL} \
                    -Dquarkus.datasource.username=${env.DB_USER} \
                    -Dquarkus.datasource.password=${env.DB_PASS} \
                    -Dquarkus.hibernate-orm.generation=update"
				}
			}
		}

		stage('Build & Test: Transferência') {
			steps {
				dir('servico-transferencia') {
					sh "mvn clean verify \
                    -Dquarkus.datasource.jdbc.url=${env.URL_TRANSFERENCIA} \
                    -Dquarkus.oidc.auth-server-url=${env.OIDC_URL} \
                    -Dkafka.bootstrap.servers=${env.KAFKA_URL} \
                    -Dquarkus.datasource.username=${env.DB_USER} \
                    -Dquarkus.datasource.password=${env.DB_PASS} \
                    -Dquarkus.hibernate-orm.generation=drop-and-create"
				}
			}
		}

		stage('Build & Test: Notificações') {
			steps {
				dir('servico-notificacoes') {
					sh "mvn clean verify \
                    -Dquarkus.datasource.jdbc.url=${env.URL_NOTIFICACAO} \
                    -Dquarkus.oidc.auth-server-url=${env.OIDC_URL} \
                    -Dkafka.bootstrap.servers=${env.KAFKA_URL} \
                    -Dquarkus.datasource.username=${env.DB_USER} \
                    -Dquarkus.datasource.password=${env.DB_PASS}"
				}
			}
		}

		stage('Sonar: Cadastro') {
			steps {
				dir('servico-cadastro') {
					withSonarQubeEnv('SonarQubeServer') {
						sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-cadastro"
					}
					// 🎯 O waitForQualityGate aqui vai ler APENAS o report desta pasta
					timeout(time: 5, unit: 'MINUTES') {
						waitForQualityGate abortPipeline: true
					}
				}
			}
		}

		stage('Sonar: Transferência') {
			steps {
				dir('servico-transferencia') {
					withSonarQubeEnv('SonarQubeServer') {
						sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-transferencia"
					}
					// 🎯 O isolamento do 'dir' e do estágio força o Jenkins a pegar o ID 'AZy2dzFkJnEsPQrVi_TT'
					timeout(time: 5, unit: 'MINUTES') {
						waitForQualityGate abortPipeline: true
					}
				}
			}
		}

		stage('Sonar: Notificações') {
			steps {
				dir('servico-notificacoes') {
					withSonarQubeEnv('SonarQubeServer') {
						sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-notificacoes"
					}
					timeout(time: 5, unit: 'MINUTES') {
						waitForQualityGate abortPipeline: true
					}
				}
			}
		}

		/* ESTÁGIO COMENTADO PARA POUPAR ESPAÇO EM DISCO
       stage('Docker: Build (Pausado)') {
          steps {
             script {
                echo "Estágio de criação de imagens Docker desativado para economizar armazenamento."
                // dir('servico-cadastro') { sh "docker build -t bb-bank/servico-cadastro:latest ." }
                // dir('servico-transferencia') { sh "docker build -t bb-bank/servico-transferencia:latest ." }
             }
          }
       }
       */

	}
}