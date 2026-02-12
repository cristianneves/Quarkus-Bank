pipeline {
	agent any

	tools {
		// O nome aqui deve ser IDÊNTICO ao que você colocou no Passo 1
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
                    -Dquarkus.hibernate-orm.database.generation=update"
				}
			}
		}

		stage('Build & Test: Transferência') {
			steps {
				dir('servico-transferencia') {
					sh "mvn clean verify \
                -Dquarkus.datasource.jdbc.url=${env.URL_TRANSFERENCIA} \
                -Dkafka.bootstrap.servers=redpanda-estavel:29092 \
                -Dquarkus.datasource.username=${env.DB_USER} \
                -Dquarkus.datasource.password=${env.DB_PASS} \
                -Dquarkus.hibernate-orm.database.generation=drop-and-create"
				}
			}
		}

		stage('SonarQube: Analisar Tudo') {
			steps {
				// Aqui rodamos a análise na raiz ou por serviço
				dir('servico-transferencia') {
					script {
						withSonarQubeEnv('SonarQubeServer') {
							sh "mvn sonar:sonar -Dsonar.projectKey=bb-transferencias"
						}
					}
				}
			}
		}
	}
}