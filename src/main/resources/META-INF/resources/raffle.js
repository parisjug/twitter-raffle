let globalWinners = [];
let globalCurrentWinner = -1;

function performRaffle() {
    const speaker = document.getElementById('speaker').value;
    fetch("/raffle?speaker=" + speaker)
        .then(response => response.json())
        .then(winners => showWinners(winners));
}

function showWinners(winners) {
    globalWinners = winners;
    showNextWinner();
}

function showNextWinner() {
    globalCurrentWinner++;
    if (globalCurrentWinner >= globalWinners.length) {
        // No more winners
        showNoWinnerFound();
        return;
    }
    // Get next winner
    const winner = globalWinners[globalCurrentWinner];
    const postUrl = winner.postUrl;
    // Request embedded post HTML code
    const url = "/embed?url=" + encodeURI(postUrl);
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'json';
    xhr.onload = function () {
        var status = xhr.status;
        if (status === 200) {
            console.log(xhr.response);
            // Ensure winner panel it shown
            document.getElementById('home').classList.add('hidden');
            document.getElementById('winner').classList.remove('hidden');
            // Update winner panel
            document.getElementById('winner-name').innerHTML = winner.name + ' (<cite>@' + winner.screenName + '</cite>)';
            const postElement = document.getElementById('post');
            postElement.innerHTML = xhr.response.html + '<p><a href="' + postUrl + '" target="_blank">View on Bluesky</a></p>';
        } else {
            console.log('Status: ' + status);
        }
    };
    xhr.send();
}

function showNoWinnerFound() {

}
