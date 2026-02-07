pipeline {
	agent any

	environment {
		SONAR_TOKEN = credentials('sonar-token')
		// 🚀 IPs para o Jenkins (que está dentro do Docker) falar com os serviços
		DB_URL = "jdbc:postgresql://172.17.0.1:5435/bank_db"
		KAFKA_HOST = "172.17.0.1:9092"
	}

	stages {
		stage('Análise de Alterações') {
			steps {
				echo 'Iniciando pipeline multi-serviço...'
			}
		}

		stage('Build & Test: Cadastro') {
			steps {
				dir('servico-cadastro') {
					sh "mvn clean verify -Dquarkus.datasource.jdbc.url=${env.DB_URL} -Dquarkus.datasource.username=quarkus -Dquarkus.datasource.password=quarkus -Dquarkus.hibernate-orm.database.generation=update"
				}
			}
		}

		stage('Build & Test: Transferência') {
			steps {
				dir('servico-transferencia') {
					sh "mvn clean verify -Dquarkus.datasource.jdbc.url=${env.DB_URL} -Dquarkus.datasource.username=quarkus -Dquarkus.datasource.password=quarkus -Dquarkus.hibernate-orm.database.generation=update -Dkafka.bootstrap.servers=${env.KAFKA_HOST}"
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