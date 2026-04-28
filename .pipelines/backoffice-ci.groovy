def call(Map params) {
    if (!params.isFromOriginalRepository) {
        echo "Skip pipeline due to changes not come from original repository."
        return
    }

    stage('Build') {
        dir('backoffice') {
            sh '''
            npm ci
            npm run build
            '''
        }
    }

    stage('Code Quality') {
        parallel {
            stage('Lint') {
                dir('backoffice') {
                    sh '''
                    npm run lint
                    '''
                }
            }

            stage('Format Check') {
                dir('backoffice') {
                    sh 'npx prettier --check .'
                }
            }
        }
    }

    stage('Audit') {
        dir('backoffice') {
            sh '''
            npm audit --omit=dev || true
            '''
        }
    }
}

return this