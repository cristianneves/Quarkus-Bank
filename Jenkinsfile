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
					// 1. Analisa e BLOQUEIA o Cadastro
					withSonarQubeEnv('SonarQubeServer') {
						dir('servico-cadastro') {
							sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-cadastro"
						}
					}
					timeout(time: 5, unit: 'MINUTES') {
						def qgCadastro = waitForQualityGate()
						if (qgCadastro.status != 'OK') {
							error "❌ Cadastro reprovado: ${qgCadastro.status} (Cover: 88.1%)"
						}
					}

					// 2. Analisa e BLOQUEIA a Transferência
					withSonarQubeEnv('SonarQubeServer') {
						dir('servico-transferencia') {
							sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-transferencia"
						}
					}
					timeout(time: 5, unit: 'MINUTES') {
						def qgTransf = waitForQualityGate()
						if (qgTransf.status != 'OK') {
							error "❌ Transferência reprovada: ${qgTransf.status} (Cover: 58.3%)"
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