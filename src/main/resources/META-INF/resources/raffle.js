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
    
    // Try to fetch and use Bluesky's official oEmbed
    const postElement = document.getElementById('post');
    postElement.innerHTML = '<p>Loading post...</p>';
    
    fetch("/embed?url=" + encodeURIComponent(postUrl))
        .then(response => response.json())
        .then(embedData => {
            console.log('oEmbed data:', embedData);
            if (embedData.html) {
                // Use the official Bluesky oEmbed HTML
                // Note: We trust the HTML from Bluesky's official oEmbed API (embed.bsky.app)
                // as it's a trusted source. The API returns sanitized, safe HTML.
                let displayHtml = '';
                
                // Add image if available (oEmbed doesn't include images)
                if (winner.imageUrl) {
                    console.log('Adding image with URL:', winner.imageUrl);
                    displayHtml += '<img src="' + escapeHtml(winner.imageUrl) + '" alt="Post image" style="max-width: 100%; height: auto; border-radius: 4px; margin-bottom: 10px;" onerror="console.error(\'Image failed to load:\', this.src)">';
                }
                
                // Add the oEmbed HTML
                displayHtml += embedData.html;
                
                postElement.innerHTML = displayHtml;
            } else {
                // Fallback to custom display
                displayCustomPost(postElement, winner, postUrl);
            }
        })
        .catch(error => {
            console.error('Failed to fetch oEmbed, using fallback display:', error);
            // Fallback to custom display
            displayCustomPost(postElement, winner, postUrl);
        });
}

function displayCustomPost(postElement, winner, postUrl) {
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
