pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'java21'
    }

    environment {
        // Credencial criada no Jenkins com o Token do Sonar
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        // 1. O Checkout Dinâmico (Usa a branch que disparou o build)
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // 2. Compilação e Testes (Gera o relatório Jacoco para o Sonar)
        stage('Compilação e Testes') {
            steps {
                // O 'verify' roda os testes de integração e o Jacoco
                sh "mvnw clean verify"
            }
        }

        // 3. Análise SonarQube (Envia os resultados dos testes e cobertura)
        stage('Análise SonarQube') {
            steps {
                script {
                    // 'SonarQubeServer' é o nome que você configurou no Jenkins
                    withSonarQubeEnv('SonarQubeServer') {
                        sh "mvnw sonar:sonar \
                            -Dsonar.projectKey=bb-transferencias \
                            -Dsonar.projectName='BB - Microserviço de Transferência' \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml \
                            -Dsonar.branch.name=${env.BRANCH_NAME}"
                    }
                }
            }
        }

        // 4. Quality Gate (O Jenkins espera o veredito do Sonar)
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    // Aborta se a cobertura for baixa ou houver bugs críticos
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // 5. Build do Artefato Final (Só chega aqui se a qualidade for 100%)
        stage('Build do Pacote (JAR)') {
            steps {
                sh 'mvnw package -DskipTests'
            }
        }

        // 6. Criação da Imagem Docker
        stage('Build Docker Image') {
            steps {
                script {
                    // Usa o Dockerfile que o Quarkus gera automaticamente
                    sh "docker build -f src/main/docker/Dockerfile.jvm -t bb/servico-transferencia:${env.BRANCH_NAME} ."
                }
            }
        }
    }

    post {
        success {
            echo '✅ Build e Análise concluídos com sucesso!'
        }
        failure {
            echo '❌ Falha na pipeline. Verifique os testes ou o Quality Gate no Sonar.'
        }
    }
}