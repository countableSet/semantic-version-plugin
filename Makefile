default:
	docker run --rm -v $$PWD:/antora -t antora/antora antora-playbook.yml
