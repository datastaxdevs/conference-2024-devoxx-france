# LLM with Gemma on GKE

## Prepare the GKE Cluster

gcloud services enable container.googleapis.com

gcloud config set project devoxxfrance
export PROJECT_ID=$(gcloud config get project)
export REGION=europe-west1
export CLUSTER_NAME=tgi2
export HF_TOKEN=HF_TOKEN

gcloud container clusters create-auto ${CLUSTER_NAME} \
  --project=${PROJECT_ID} \
  --region=${REGION} \
  --release-channel=rapid \
  --cluster-version=1.28

kubectl create secret generic hf-secret \
--from-literal=hf_api_token=${HF_TOKEN} \
--dry-run=client -o yaml | kubectl apply -f -

kubectx gke_devoxxfrance_europe-west1_tgi2

kubectl apply -f k8s/manifests.yaml

kubectl logs -f -l app=gemma-server

kubectl port-forward service/llm-service 8000:8000

USER_PROMPT="Java is a"

curl -X POST http://localhost:8000/generate \
  -H "Content-Type: application/json" \
  -d @- <<EOF
{
    "inputs": "${USER_PROMPT}",
    "parameters": {
        "temperature": 0.90,
        "top_p": 0.95,
        "max_new_tokens": 128
    }
}
EOF

export language="French"
export character="A programmer"
export setting="Learning how to write Java code"
export plot="Keeps getting a lot of error messages"

BED_STORY_PROMPT="You are a creative and passionate story teller for kids.

                    Kids love hearing about the stories you invent.
                    Your stories are split into 5 acts:
                    - Act 1 : Sets up the story providing any contextual background the reader needs, but most importantly it contains the inciting moment. This incident sets the story in motion. An incident forces the protagonist to react. It requires resolution, producing narrative tension.
                    - Act 2 : On a simplistic level this is the obstacles that are placed in the way of the protagonists as they attempt to resolve the inciting incident.
                    - Act 3 : This is the turning point of the story. It is the point of the highest tension. In many modern narratives, this is the big battle or showdown.
                    - Act 4 : The falling action is that part of the story in which the main part (the climax) has finished and you're heading to the conclusion. This is the calm after the tension of the climax.
                    - Act 5 : This is the resolution of the story where conflicts are resolved and loose ends tied up. This is the moment of emotional release for the reader.

                    Generate a kid story in ${language} language in 5 acts, with around 20 sentences per act, where the protagonist is ${character}, where the action takes place ${setting} and the plot is about ${plot}.
                    "

curl -s -X POST http://localhost:8000/generate \
  -H "Content-Type: application/json" \
  -d @- <<EOF | jq .generated_text
{
    "inputs": "${BED_STORY_PROMPT}",
    "parameters": {
        "temperature": 0.90,
        "top_p": 0.95,
        "max_new_tokens": 256
    }
}
EOF
