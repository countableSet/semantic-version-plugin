default:
	docker run --rm -v $$PWD:/antora -t antora/antora antora-playbook.yml

pages:
	npm i -g antora@3.0.1
	antora -v
	antora antora-playbook.yml
