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
    
    // Debug logging
    console.log('Winner data:', winner);
    console.log('Image URL:', winner.imageUrl);
    
    // Ensure winner panel is shown
    document.getElementById('home').classList.add('hidden');
    document.getElementById('winner').classList.remove('hidden');
    
    // Update winner panel
    document.getElementById('winner-name').innerHTML = winner.name + ' (<cite>@' + winner.screenName + '</cite>)';
    
    // Build the post content HTML
    const postElement = document.getElementById('post');
    let postHtml = '<div class="bluesky-post-embed" style="border: 1px solid #ccc; padding: 15px; border-radius: 8px; margin: 20px 0; background: #f9f9f9;">';
    
    // Add post text if available
    if (winner.postText) {
        postHtml += '<p style="font-size: 16px; line-height: 1.5; margin-bottom: 10px;">' + escapeHtml(winner.postText) + '</p>';
    }
    
    // Add image if available
    if (winner.imageUrl) {
        console.log('Adding image with URL:', winner.imageUrl);
        postHtml += '<img src="' + escapeHtml(winner.imageUrl) + '" alt="Post image" style="max-width: 100%; height: auto; border-radius: 4px; margin-bottom: 10px;" onerror="console.error(\'Image failed to load:\', this.src)">';
    } else {
        console.log('No image URL available');
    }
    
    // Add link to view on Bluesky
    postHtml += '<p style="margin-top: 10px;"><a href="' + escapeHtml(postUrl) + '" target="_blank" style="color: #0085ff; text-decoration: none;">View on Bluesky →</a></p>';
    postHtml += '</div>';
    
    postElement.innerHTML = postHtml;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNoWinnerFound() {

}
