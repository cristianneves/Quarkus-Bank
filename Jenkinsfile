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

		stage('Sonar: Cadastro') {
			steps {
				withSonarQubeEnv('SonarQubeServer') {
					dir('servico-cadastro') {
						sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-cadastro"
					}
				}
				timeout(time: 5, unit: 'MINUTES') {
					// 🎯 O waitForQualityGate aqui vai ler APENAS o report do cadastro
					waitForQualityGate abortPipeline: true
				}
			}
		}

		stage('Sonar: Transferência') {
			steps {
				withSonarQubeEnv('SonarQubeServer') {
					dir('servico-transferencia') {
						sh "chmod +x ./mvnw && ./mvnw sonar:sonar -Dsonar.projectKey=servico-transferencia"
					}
				}
				timeout(time: 5, unit: 'MINUTES') {
					// 🎯 O waitForQualityGate aqui será forçado a pegar o novo ID da transferência
					waitForQualityGate abortPipeline: true
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