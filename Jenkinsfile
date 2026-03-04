pipeline {
	agent any

	tools {
		maven 'maven'
	}

	environment {
		SONAR_TOKEN = credentials('sonar-token')

		URL_CADASTRO = "jdbc:postgresql://db-cadastro:5432/cadastro_db"
		URL_TRANSFERENCIA = "jdbc:postgresql://db-transferencia:5432/transferencia_db"
		OIDC_URL = "http://keycloak-estavel:8080/realms/bank-realm"
		KAFKA_URL = "redpanda-estavel:29092"

		DB_USER = "admin"
		DB_PASS = "admin"
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

		stage('SonarQube: Análise e Bloqueio') {
			steps {
				script {
					// 1. Analisa Cadastro
					withSonarQubeEnv('SonarQubeServer') {
						dir('servico-cadastro') {
							sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-cadastro"
						}
					}
					// 2. Analisa Transferência
					withSonarQubeEnv('SonarQubeServer') {
						dir('servico-transferencia') {
							sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-transferencia"
						}
					}

					// 🚀 O pulo do gato: O Jenkins vai esperar o veredito de AMBOS
					// Se qualquer um dos dois (ou os dois) falhar no Gate, o pipeline morre aqui.
					timeout(time: 10, unit: 'MINUTES') {
						def qg = waitForQualityGate()
						if (qg.status != 'OK') {
							error "❌ Quality Gate falhou! Verifique o SonarQube. Um dos serviços está abaixo de 80%."
						}
					}
				}
			}
		}

		stage("Check Quality Gate") {
			steps {
				script {
					timeout(time: 1, unit: 'HOURS') {
						def qg = waitForQualityGate()
						if (qg.status != 'OK') {
							error "❌ Quality Gate falhou: ${qg.status}. O build foi abortado para proteger a produção!"
						}
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